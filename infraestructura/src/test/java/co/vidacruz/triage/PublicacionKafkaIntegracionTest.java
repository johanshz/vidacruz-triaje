package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;
import co.vidacruz.triage.evento.comando.ManejadorPublicarOutbox;
import co.vidacruz.triage.vigilancia.comando.ComandoEscalarVencidos;
import co.vidacruz.triage.vigilancia.comando.ManejadorEscalarVencidos;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.List;
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
import io.micrometer.core.instrument.MeterRegistry;
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
                        "--spring.kafka.producer.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                        "--vidacruz.outbox.intervalo=PT1H", "--vidacruz.outbox.retraso-inicial=PT1H")) {
            assertThat(contexto.getEnvironment().getProperty("spring.kafka.bootstrap-servers"))
                    .isEqualTo(KAFKA.getBootstrapServers());
            try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
                var tema = admin.describeTopics(java.util.List.of("triaje.eventos")).allTopicNames().get();
                assertThat(tema.get("triaje.eventos").partitions()).hasSize(3);
            }
            DatabaseClient db = contexto.getBean(DatabaseClient.class);
            ManejadorPublicarOutbox publicador = contexto.getBean(ManejadorPublicarOutbox.class);
            ManejadorEscalarVencidos escalador = contexto.getBean(ManejadorEscalarVencidos.class);
            UUID primerPaciente = UUID.randomUUID();
            UUID segundoPaciente = UUID.randomUUID();
            UUID idRegistro = UUID.randomUUID();
            UUID idConsulta = UUID.randomUUID();
            UUID idAlta = UUID.randomUUID();
            UUID pacienteEscalado = UUID.randomUUID();
            StepVerifier.create(db.sql("UPDATE outbox SET enviado_en=now() WHERE enviado_en IS NULL")
                    .fetch().rowsUpdated()).expectNextMatches(actualizadas -> actualizadas >= 0).verifyComplete();
            insertarEvento(db, idRegistro, primerPaciente, "paciente_registrado", null, "REGISTRADO");
            insertarEvento(db, idConsulta, primerPaciente, "paciente_en_consulta", "REGISTRADO", "EN_CONSULTA");
            insertarEvento(db, idAlta, segundoPaciente, "paciente_registrado", null, "REGISTRADO");
            insertarPacienteVencido(db, pacienteEscalado);
            StepVerifier.create(escalador.ejecutar(new ComandoEscalarVencidos())).verifyComplete();
            var idEscalamientoEncontrado = new java.util.concurrent.atomic.AtomicReference<UUID>();
            StepVerifier.create(db.sql("SELECT id FROM outbox WHERE llave = :llave AND tipo = 'paciente_escalado'")
                    .bind("llave", pacienteEscalado.toString())
                    .map((fila, metadata) -> fila.get("id", UUID.class)).one())
                    .assertNext(idEscalamientoEncontrado::set).verifyComplete();
            UUID idEscalamiento = idEscalamientoEncontrado.get();
            assertThat(idEscalamiento).isNotNull();
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
                var idsRecibidos = new java.util.ArrayList<String>();
                var sobres = new java.util.HashMap<String, com.fasterxml.jackson.databind.JsonNode>();
                while (System.nanoTime() < vence) {
                    for (var mensaje : consumidor.poll(Duration.ofMillis(250)).records("triaje.eventos")) {
                        var sobre = new com.fasterxml.jackson.databind.ObjectMapper().readTree(mensaje.value());
                        String idEvento = sobre.path("idEvento").asText();
                        if (List.of(idRegistro.toString(), idConsulta.toString(), idAlta.toString(),
                                idEscalamiento.toString()).contains(idEvento)) {
                            assertThat(mensaje.key()).isEqualTo(sobre.path("datos").path("idPaciente").asText());
                            idsRecibidos.add(idEvento);
                            sobres.put(idEvento, sobre);
                        }
                    }
                }
                assertThat(idsRecibidos).containsExactlyInAnyOrder(idRegistro.toString(), idConsulta.toString(),
                        idAlta.toString(), idEscalamiento.toString());
                assertThat(idsRecibidos.indexOf(idRegistro.toString())).isLessThan(idsRecibidos.indexOf(idConsulta.toString()));
                assertThat(sobres.get(idRegistro.toString()).path("tipo").asText()).isEqualTo("paciente_registrado");
                assertThat(sobres.get(idConsulta.toString()).path("tipo").asText()).isEqualTo("paciente_en_consulta");
                assertThat(sobres.get(idAlta.toString()).path("tipo").asText()).isEqualTo("paciente_registrado");
                assertThat(sobres.get(idRegistro.toString()).path("version").asInt()).isEqualTo(1);
                assertThat(sobres.get(idRegistro.toString()).path("ocurridoEn").asText()).matches(".*Z$");
                assertThat(sobres.get(idConsulta.toString()).path("datos").path("escalado").asBoolean()).isFalse();
                assertThat(sobres.get(idConsulta.toString()).path("datos").path("estadoAnterior").asText()).isEqualTo("REGISTRADO");
                assertThat(sobres.get(idConsulta.toString()).path("datos").path("estadoNuevo").asText()).isEqualTo("EN_CONSULTA");
                assertThat(sobres.get(idAlta.toString()).path("datos").path("estadoAnterior").isNull()).isTrue();
                var sobreEscalamiento = sobres.get(idEscalamiento.toString());
                assertThat(sobreEscalamiento.path("tipo").asText()).isEqualTo("paciente_escalado");
                assertThat(sobreEscalamiento.path("version").asInt()).isEqualTo(1);
                assertThat(sobreEscalamiento.path("datos").path("idPaciente").asText())
                        .isEqualTo(pacienteEscalado.toString());
                assertThat(sobreEscalamiento.path("datos").path("escalado").asBoolean()).isTrue();
                Instant ocurridaEn = Instant.parse(sobreEscalamiento.path("ocurridoEn").asText());
                StepVerifier.create(db.sql("SELECT ocurrido_en FROM auditoria WHERE paciente_id = :id "
                        + "AND tipo_evento = 'paciente_escalado'").bind("id", pacienteEscalado)
                        .map((fila, metadata) -> fila.get("ocurrido_en", Instant.class)).one())
                        .expectNext(ocurridaEn).verifyComplete();
            }
            StepVerifier.create(db.sql("SELECT COUNT(*) AS enviados FROM outbox WHERE id IN (:registro,:consulta,:alta) AND enviado_en IS NOT NULL")
                    .bind("registro", idRegistro).bind("consulta", idConsulta).bind("alta", idAlta)
                    .map((row, metadata) -> row.get("enviados", Long.class)).one()).expectNext(3L).verifyComplete();
            assertThat(contexto.getBean(MeterRegistry.class).get("triaje.outbox.pendientes").gauge().value()).isZero();
        }
    }

    private void insertarEvento(DatabaseClient db, UUID id, UUID paciente, String tipo,
            String estadoAnterior, String estadoNuevo) {
        String anterior = estadoAnterior == null ? "null" : "\"" + estadoAnterior + "\"";
        String carga = "{\"idEvento\":\"" + id + "\",\"tipo\":\"" + tipo + "\",\"version\":1,"
                + "\"ocurridoEn\":\"2026-11-06T00:00:00Z\",\"datos\":{\"idPaciente\":\"" + paciente
                + "\",\"nivel\":\"URGENTE\",\"estadoAnterior\":" + anterior + ",\"estadoNuevo\":\""
                + estadoNuevo + "\",\"escalado\":false}}";
        StepVerifier.create(db.sql("INSERT INTO outbox (id,tipo,llave,carga) VALUES (:id,:tipo,:llave,CAST(:carga AS jsonb))")
                .bind("id", id).bind("tipo", tipo).bind("llave", paciente.toString()).bind("carga", carga)
                .fetch().rowsUpdated()).expectNext(1L).verifyComplete();
    }

    private void insertarPacienteVencido(DatabaseClient db, UUID id) {
        Instant registro = Instant.now().minusSeconds(360);
        StepVerifier.create(db.sql("""
                INSERT INTO paciente (id, nombre_completo, edad, frecuencia_cardiaca, presion_sistolica,
                    presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal, nivel,
                    sla_minutos, estado, escalado, fecha_registro, fecha_estado)
                VALUES (:id, 'Paciente escalado por job', 30, 80, 120, 80, 98, 36.5, 0, 15, 'Dolor',
                    'URGENTE', 5, 'REGISTRADO', false, :fecha, :fecha)
                """).bind("id", id).bind("fecha", registro).fetch().rowsUpdated())
                .expectNext(1L).verifyComplete();
    }
}
