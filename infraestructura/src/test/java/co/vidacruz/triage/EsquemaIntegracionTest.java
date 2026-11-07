package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureObservability
@Import(ControladorErroresPrueba.class)
class EsquemaIntegracionTest extends PruebaIntegracionPostgres {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void flywayCreaLasTresTablasVisiblesPorR2dbc() {
        StepVerifier.create(databaseClient.sql("SELECT to_regclass('public.paciente')::text")
                .map((row, metadata) -> row.get(0, String.class)).one())
            .assertNext(tabla -> assertEquals("paciente", tabla))
            .verifyComplete();

        StepVerifier.create(databaseClient.sql("SELECT to_regclass('public.auditoria')::text")
                .map((row, metadata) -> row.get(0, String.class)).one())
            .assertNext(tabla -> assertEquals("auditoria", tabla))
            .verifyComplete();

        StepVerifier.create(databaseClient.sql("SELECT to_regclass('public.outbox')::text")
                .map((row, metadata) -> row.get(0, String.class)).one())
            .assertNext(tabla -> assertEquals("outbox", tabla))
            .verifyComplete();
    }

    @Test
    void auditoriaPermiteInsertar() {
        UUID pacienteId = UUID.randomUUID();
        StepVerifier.create(insertarPaciente(pacienteId).then(insertarAuditoria(pacienteId)))
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    void auditoriaRechazaUpdateDeleteYTruncate() {
        UUID pacienteId = UUID.randomUUID();
        UUID auditoriaId = UUID.randomUUID();
        StepVerifier.create(insertarPaciente(pacienteId).then(insertarAuditoria(pacienteId, auditoriaId)))
            .expectNext(1L)
            .verifyComplete();

        verificarAuditoriaInmutable(databaseClient.sql("UPDATE auditoria SET tipo_evento = 'paciente_alta' WHERE id = :id")
            .bind("id", auditoriaId).fetch().rowsUpdated());
        verificarAuditoriaInmutable(databaseClient.sql("DELETE FROM auditoria WHERE id = :id")
            .bind("id", auditoriaId).fetch().rowsUpdated());
        verificarAuditoriaInmutable(databaseClient.sql("TRUNCATE auditoria").fetch().rowsUpdated());
    }

    @Test
    void checkRechazaCatalogosDesconocidos() {
        UUID pacienteId = UUID.randomUUID();
        StepVerifier.create(insertarPaciente(pacienteId, "NIVEL_INVALIDO"))
            .expectError()
            .verify();
        StepVerifier.create(insertarPaciente(UUID.randomUUID(), "URGENTE", "ESTADO_INVALIDO"))
            .expectError()
            .verify();
        StepVerifier.create(databaseClient.sql("INSERT INTO auditoria (id, paciente_id, tipo_evento, ocurrido_en) "
                + "VALUES (:id, :paciente, 'tipo_invalido', now())")
            .bind("id", UUID.randomUUID()).bind("paciente", pacienteId).fetch().rowsUpdated())
            .expectError()
            .verify();
        StepVerifier.create(insertarOutbox(UUID.randomUUID(), "tipo_invalido"))
            .expectError()
            .verify();
    }

    @Test
    void outboxAsignaSecuenciaCreciente() {
        UUID pacienteId = UUID.randomUUID();
        StepVerifier.create(insertarOutbox(pacienteId, "paciente_registrado")
                .flatMap(primera -> insertarOutbox(pacienteId, "paciente_alta")
                    .map(segunda -> primera < segunda)))
            .assertNext(secuenciaCreciente -> assertTrue(secuenciaCreciente))
            .verifyComplete();
    }

    @Test
    void actuatorInfoRequiereClaveYSeExponeConClaveValida() {
        webTestClient.get().uri("/actuator/info").exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO");
        webTestClient.get().uri("/actuator/info").exchange()
            .expectHeader().exists("X-Trace-Id");

        webTestClient.get().uri("/actuator/info").header("X-API-Key", "clave-incorrecta").exchange()
            .expectStatus().isUnauthorized().expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO");

        webTestClient.get().uri("/actuator/info").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isOk().expectHeader().exists("X-Trace-Id");
        webTestClient.get().uri("/actuator/prometheus").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isOk();
    }

    @Test
    void probesHealthEstanAbiertasYReadinessVerificaPostgres() {
        webTestClient.get().uri("/actuator/health/liveness").exchange()
            .expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.components").doesNotExist();
        webTestClient.get().uri("/actuator/health/readiness").exchange()
            .expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.components").doesNotExist();
    }

    @Test
    void erroresDeDominioSeSerializanComoProblemDetail() {
        webTestClient.get().uri("/pruebas/error/validacion").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isBadRequest().expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
            .jsonPath("$.campo").isEqualTo("edad").jsonPath("$.traceId").exists();

        webTestClient.get().uri("/pruebas/error/no-encontrado").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isNotFound().expectBody().jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO");

        webTestClient.get().uri("/pruebas/error/conflicto").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isEqualTo(409).expectBody().jsonPath("$.codigo").isEqualTo("TRANSICION_INVALIDA");

        webTestClient.get().uri("/ruta-inexistente").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isNotFound().expectBody().jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO");

        webTestClient.get().uri("/pruebas/entrada").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isEqualTo(405).expectBody().jsonPath("$.codigo").isEqualTo("SOLICITUD_INVALIDA");

        webTestClient.post().uri("/pruebas/entrada").header("X-API-Key", "clave-prueba")
            .contentType(MediaType.TEXT_PLAIN).bodyValue("dato")
            .exchange().expectStatus().isEqualTo(415)
            .expectBody().jsonPath("$.codigo").isEqualTo("SOLICITUD_INVALIDA");
    }

    @Test
    void jsonMalFormadoOTipoIncorrectoEsUnErrorDeValidacion() {
        webTestClient.post().uri("/pruebas/entrada").header("X-API-Key", "clave-prueba")
            .contentType(MediaType.APPLICATION_JSON).bodyValue("{")
            .exchange().expectStatus().isBadRequest().expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION");


        webTestClient.post().uri("/pruebas/entrada").header("X-API-Key", "clave-prueba")
            .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"edad\":1.5}")
            .exchange().expectStatus().isBadRequest().expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
            .jsonPath("$.campo").isEqualTo("edad");
    }

    @Test
    void respuestasDeErrorLlevanTraceIdYErroresInternosNoExponenLaTraza() {
        webTestClient.get().uri("/pruebas/error/interno").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().is5xxServerError().expectHeader().exists("X-Trace-Id")
            .expectBody().jsonPath("$.codigo").isEqualTo("ERROR_INTERNO")
            .jsonPath("$.detail").isEqualTo("Ocurrió un error interno")
            .jsonPath("$.traceId").exists();
    }

    @Test
    void configuracionEliminaDbUrlYActivaTrazasEcs() throws IOException {
        try (var recurso = getClass().getResourceAsStream("/application.yaml")) {
            assertTrue(recurso != null);
            String configuracion = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(configuracion.contains("${DB_HOST}"));
            assertTrue(configuracion.contains("${DB_NAME}"));
            assertFalse(configuracion.contains("DB_URL"));
            assertTrue(configuracion.contains("console: ecs"));
            assertTrue(configuracion.contains("context-propagation: auto"));
        }
    }

    private reactor.core.publisher.Mono<Long> insertarPaciente(UUID id) {
        return insertarPaciente(id, "URGENTE", "REGISTRADO");
    }

    private reactor.core.publisher.Mono<Long> insertarPaciente(UUID id, String nivel) {
        return insertarPaciente(id, nivel, "REGISTRADO");
    }

    private reactor.core.publisher.Mono<Long> insertarPaciente(UUID id, String nivel, String estado) {
        return databaseClient.sql("INSERT INTO paciente (id, nombre_completo, edad, frecuencia_cardiaca, "
                + "presion_sistolica, presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal, "
                + "nivel, sla_minutos, estado, fecha_registro, fecha_estado) VALUES (:id, 'Paciente Prueba', 30, "
                + "80, 120, 80, 98, 36.5, 0, 15, 'Dolor de cabeza', :nivel, 30, :estado, :fecha, :fecha)")
            .bind("id", id).bind("nivel", nivel).bind("estado", estado)
            .bind("fecha", OffsetDateTime.now(ZoneOffset.UTC)).fetch().rowsUpdated();
    }

    private reactor.core.publisher.Mono<Long> insertarAuditoria(UUID pacienteId) {
        return insertarAuditoria(pacienteId, UUID.randomUUID());
    }

    private reactor.core.publisher.Mono<Long> insertarAuditoria(UUID pacienteId, UUID auditoriaId) {
        return databaseClient.sql("INSERT INTO auditoria (id, paciente_id, tipo_evento, estado_nuevo, ocurrido_en) "
                + "VALUES (:id, :paciente, 'paciente_registrado', 'REGISTRADO', :fecha)")
            .bind("id", auditoriaId).bind("paciente", pacienteId)
            .bind("fecha", OffsetDateTime.now(ZoneOffset.UTC)).fetch().rowsUpdated();
    }

    private reactor.core.publisher.Mono<Long> insertarOutbox(UUID pacienteId, String tipo) {
        return databaseClient.sql("INSERT INTO outbox (id, tipo, llave, carga) "
                + "VALUES (:id, :tipo, :llave, '{}'::jsonb) RETURNING secuencia")
            .bind("id", UUID.randomUUID()).bind("tipo", tipo).bind("llave", pacienteId.toString())
            .map((row, metadata) -> row.get("secuencia", Long.class)).one();
    }

    private void verificarAuditoriaInmutable(reactor.core.publisher.Mono<Long> operacion) {
        StepVerifier.create(operacion)
            .expectErrorMatches(error -> error.getMessage().contains("auditoria es inmutable"))
            .verify();
    }
}
