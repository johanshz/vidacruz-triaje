package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;
import co.vidacruz.triage.alerta.adaptador.sse.AdaptadorSseAlerta;
import co.vidacruz.triage.evento.modelo.entidad.DatosEventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.configuracion.ConfiguracionKafka;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AlertasKafkaIntegracionTest extends PruebaIntegracionPostgres {
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"));

    static {
        KAFKA.start();
    }

    @Test
    void dosGruposRecibenLaAlertaYLosMensajesInvalidosLleganALaDlt() throws Exception {
        UUID idPaciente = UUID.randomUUID();
        Instant registro = Instant.now().minusSeconds(300);
        try (ConfigurableApplicationContext replicaA = iniciarAplicacion("replica-a");
             ConfigurableApplicationContext replicaB = iniciarAplicacion("replica-b")) {
            DatabaseClient db = replicaA.getBean(DatabaseClient.class);
            StepVerifier.create(insertarPaciente(db, idPaciente, registro)).expectNext(1L).verifyComplete();
            crearTemaDlt();

            var salidaA = replicaA.getBean(AdaptadorSseAlerta.class).nuevas().next();
            var salidaB = replicaB.getBean(AdaptadorSseAlerta.class).nuevas().next();
            var publicador = replicaA.getBean(KafkaTemplate.class);
            var evento = new EventoPaciente(UUID.randomUUID(), "paciente_escalado", 1, Instant.now(),
                    new DatosEventoPaciente(idPaciente, "CRITICO", "REGISTRADO", "REGISTRADO", true));
            String carga = new ObjectMapper().findAndRegisterModules().writeValueAsString(evento);
            Mono<?> difusion = Mono.zip(salidaA, salidaB).doOnNext(alertas -> {
                assertThat(alertas.getT1().idPaciente()).isEqualTo(idPaciente);
                assertThat(alertas.getT2().idPaciente()).isEqualTo(idPaciente);
                assertThat(alertas.getT1().nivel()).isEqualTo("CRITICO");
            });
            Mono<Void> envio = Mono.delay(Duration.ofSeconds(1))
                    .then(Mono.fromFuture(publicador.send("triaje.eventos", idPaciente.toString(), carga)))
                    .then();
            StepVerifier.create(Mono.when(difusion, envio)).expectComplete().verify(Duration.ofSeconds(30));

            try (KafkaConsumer<String, String> consumidorDlt = consumidorDlt()) {
                publicador.send("triaje.eventos", "mensaje-invalido", "{").get(10, TimeUnit.SECONDS);
                String mensajeDlt = esperarMensaje(consumidorDlt);
                assertThat(mensajeDlt).isEqualTo("{");
            }
        }
    }

    private ConfigurableApplicationContext iniciarAplicacion(String instancia) {
        return new SpringApplicationBuilder(Aplicacion.class).web(WebApplicationType.REACTIVE).run(
                "--SERVER_PORT=0", "--DB_HOST=" + POSTGRES.getHost(),
                "--DB_PORT=" + POSTGRES.getMappedPort(5432), "--DB_NAME=" + POSTGRES.getDatabaseName(),
                "--DB_USER=" + POSTGRES.getUsername(), "--DB_PASSWORD=" + POSTGRES.getPassword(),
                "--TRIAGE_API_KEY=clave-prueba", "--KAFKA_BOOTSTRAP_SERVERS=" + KAFKA.getBootstrapServers(),
                "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                "--VIDACRUZ_INSTANCIA=" + instancia, "--vidacruz.outbox.intervalo=PT1H",
                "--vidacruz.vigilancia.intervalo=PT1H", "--vidacruz.vigilancia.retraso-inicial=PT1H");
    }

    private Mono<Long> insertarPaciente(DatabaseClient db, UUID id, Instant registro) {
        return db.sql("""
                INSERT INTO paciente (id, nombre_completo, edad, frecuencia_cardiaca, presion_sistolica,
                    presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal, nivel,
                    sla_minutos, estado, escalado, fecha_registro, fecha_estado)
                VALUES (:id, 'Paciente alerta', 30, 80, 120, 80, 98, 36.5, 0, 15, 'Dolor',
                    'CRITICO', 1, 'REGISTRADO', true, :registro, :registro)
                """).bind("id", id).bind("registro", registro).fetch().rowsUpdated();
    }

    private void crearTemaDlt() throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(ConfiguracionKafka.TOPIC_EVENTOS_DLT, 3, (short) 1))).all().get();
        }
    }

    private KafkaConsumer<String, String> consumidorDlt() {
        Map<String, Object> propiedades = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "verificacion-dlt-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var consumidor = new KafkaConsumer<String, String>(propiedades);
        consumidor.subscribe(List.of(ConfiguracionKafka.TOPIC_EVENTOS_DLT));
        return consumidor;
    }

    private String esperarMensaje(KafkaConsumer<String, String> consumidor) {
        long vence = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < vence) {
            var registros = consumidor.poll(Duration.ofMillis(250)).records(ConfiguracionKafka.TOPIC_EVENTOS_DLT);
            if (registros.iterator().hasNext()) {
                return registros.iterator().next().value();
            }
        }
        throw new AssertionError("No llegó el mensaje inválido a triaje.eventos.DLT");
    }
}
