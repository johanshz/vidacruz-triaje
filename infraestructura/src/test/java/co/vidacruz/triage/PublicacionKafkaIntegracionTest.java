package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;
import co.vidacruz.triage.evento.comando.ManejadorPublicarOutbox;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

class PublicacionKafkaIntegracionTest extends PruebaIntegracionPostgres {
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"));
    static { KAFKA.start(); }

    @Test void creaTopicDeTresParticionesYPublicaElSobreJsonDelOutbox() throws Exception {
        try (ConfigurableApplicationContext contexto = new SpringApplicationBuilder(Aplicacion.class)
                .web(WebApplicationType.REACTIVE).run(
                        "--SERVER_PORT=0", "--DB_HOST=" + POSTGRES.getHost(),
                        "--DB_PORT=" + POSTGRES.getMappedPort(5432), "--DB_NAME=" + POSTGRES.getDatabaseName(),
                        "--DB_USER=" + POSTGRES.getUsername(), "--DB_PASSWORD=" + POSTGRES.getPassword(),
                        "--TRIAGE_API_KEY=clave-prueba", "--KAFKA_BOOTSTRAP_SERVERS=" + KAFKA.getBootstrapServers(),
                        "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                        "--vidacruz.outbox.intervalo=PT1H", "--vidacruz.outbox.retraso-inicial=PT1H")) {
            try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
                var tema = admin.describeTopics(java.util.List.of("triaje.eventos")).allTopicNames().get();
                assertThat(tema.get("triaje.eventos").partitions()).hasSize(3);
            }
            DatabaseClient db = contexto.getBean(DatabaseClient.class);
            ManejadorPublicarOutbox publicador = contexto.getBean(ManejadorPublicarOutbox.class);
            UUID paciente = UUID.randomUUID();
            UUID evento = UUID.randomUUID();
            String carga = "{\"idEvento\":\"" + evento + "\",\"tipo\":\"paciente_registrado\",\"version\":1,\"ocurridoEn\":\"2026-11-06T00:00:00Z\",\"datos\":{\"idPaciente\":\"" + paciente + "\",\"nivel\":\"URGENTE\",\"estadoAnterior\":null,\"estadoNuevo\":\"REGISTRADO\",\"escalado\":false}}";
            StepVerifier.create(db.sql("UPDATE outbox SET enviado_en=now() WHERE enviado_en IS NULL")
                    .fetch().rowsUpdated()).expectNextCount(1).verifyComplete();
            StepVerifier.create(db.sql("INSERT INTO outbox (id,tipo,llave,carga) VALUES (:id,'paciente_registrado',:llave,CAST(:carga AS jsonb))")
                    .bind("id", evento).bind("llave", paciente.toString()).bind("carga", carga).fetch().rowsUpdated())
                    .expectNext(1L).verifyComplete();
            StepVerifier.create(publicador.ejecutar()).verifyComplete();
            StepVerifier.create(publicador.ejecutar()).verifyComplete();

            Map<String, Object> propiedadesConsumidor = Map.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                    ConsumerConfig.GROUP_ID_CONFIG, "verificacion-" + UUID.randomUUID(),
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            try (KafkaConsumer<String, String> consumidor = new KafkaConsumer<>(propiedadesConsumidor)) {
                consumidor.subscribe(java.util.List.of("triaje.eventos"));
                var vence = System.nanoTime() + Duration.ofSeconds(20).toNanos();
                int coincidencias = 0;
                while (System.nanoTime() < vence) {
                    for (var registro : consumidor.poll(Duration.ofMillis(250)).records("triaje.eventos")) {
                        var sobre = new com.fasterxml.jackson.databind.ObjectMapper().readTree(registro.value());
                        if (registro.key().equals(paciente.toString())
                                && sobre.path("idEvento").asText().equals(evento.toString())
                                && sobre.path("tipo").asText().equals("paciente_registrado")
                                && sobre.path("version").asInt() == 1
                                && sobre.path("datos").path("idPaciente").asText().equals(paciente.toString())) coincidencias++;
                    }
                }
                assertThat(coincidencias).isEqualTo(1);
            }
            StepVerifier.create(db.sql("SELECT enviado_en IS NOT NULL AS enviado FROM outbox WHERE id=:id").bind("id", evento)
                    .map((row, metadata) -> row.get("enviado", Boolean.class)).one()).expectNext(true).verifyComplete();
        }
    }
}
