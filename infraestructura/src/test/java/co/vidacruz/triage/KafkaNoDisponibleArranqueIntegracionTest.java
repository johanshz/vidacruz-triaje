package co.vidacruz.triage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.MediaType;
import java.util.UUID;
import reactor.test.StepVerifier;
import co.vidacruz.triage.evento.comando.ManejadorPublicarOutbox;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureObservability
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KafkaNoDisponibleArranqueIntegracionTest extends PruebaIntegracionPostgres {
    @Autowired private WebTestClient cliente;
    @Autowired private org.springframework.r2dbc.core.DatabaseClient database;
    @Autowired private ManejadorPublicarOutbox publicador;

    @DynamicPropertySource
    static void registrarKafkaNoDisponible(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> "127.0.0.1:1");
        registry.add("spring.kafka.admin.operation-timeout", () -> "1");
    }

    @Test void readinessSigueVerdeCuandoKafkaNoRespondeDuranteElArranque() {
        cliente.get().uri("/actuator/health/readiness").exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test void registraYCambiaEstadoSinKafkaYConservaLosEventosPendientes() throws Exception {
        StepVerifier.create(database.sql("UPDATE outbox SET enviado_en=now() WHERE enviado_en IS NULL")
                .fetch().rowsUpdated()).expectNextMatches(actualizadas -> actualizadas >= 0).verifyComplete();
        String solicitud = "{\"nombreCompleto\":\"Paciente Kafka Caido\",\"edad\":40,"
                + "\"frecuenciaCardiaca\":70,\"presionSistolica\":120,\"presionDiastolica\":80,"
                + "\"spo2\":98,\"temperatura\":36.5,\"dolor\":2,\"gcs\":15,\"quejaPrincipal\":\"Dolor\"}";
        var respuesta = cliente.post().uri("/triage/pacientes").header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(solicitud).exchange().expectStatus().isCreated()
                .expectBody().jsonPath("$.estado").isEqualTo("REGISTRADO").returnResult();
        UUID paciente = UUID.fromString(new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(respuesta.getResponseBody()).get("id").asText());

        cliente.patch().uri("/triage/pacientes/{id}/estado", paciente).header("X-API-Key", "clave-prueba")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"estado\":\"EN_CONSULTA\"}")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.estado").isEqualTo("EN_CONSULTA");
        cliente.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
        StepVerifier.create(publicador.ejecutar()).verifyComplete();
        StepVerifier.create(database.sql("SELECT COUNT(*) AS total FROM outbox WHERE llave=:id AND enviado_en IS NULL")
                .bind("id", paciente.toString()).map((row, metadata) -> row.get("total", Long.class)).one())
                .expectNext(2L).verifyComplete();
        String metricas = cliente.get().uri("/actuator/prometheus").header("X-API-Key", "clave-prueba")
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        String muestra = metricas.lines().filter(linea -> linea.startsWith("triaje_outbox_pendientes "))
                .findFirst().orElseThrow();
        assertEquals(2.0, Double.parseDouble(muestra.substring(muestra.lastIndexOf(' ') + 1)), 0.0);
    }
}
