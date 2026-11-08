package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureObservability
class SeguridadActuatorIntegracionTest extends SoporteIntegracionTriage {
    @Test
    void actuatorInfoRequiereClaveYSeExponeConClaveValida() throws IOException {
        var sinClave = webTestClient.get().uri("/actuator/info").exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO")
            .jsonPath("$.detail").isEqualTo("API key ausente o inválida").returnResult();
        assertEquals(sinClave.getResponseHeaders().getFirst("X-Trace-Id"),
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(sinClave.getResponseBody()).get("traceId").asText());
        var prometheusSinClave = webTestClient.get().uri("/actuator/prometheus").exchange().expectStatus().isUnauthorized()
            .expectBody().jsonPath("$.detail").isEqualTo("API key ausente o inválida")
            .jsonPath("$.traceId").exists().returnResult();
        assertEquals(prometheusSinClave.getResponseHeaders().getFirst("X-Trace-Id"),
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(prometheusSinClave.getResponseBody())
                .get("traceId").asText());

        webTestClient.get().uri("/actuator/info").header("X-API-Key", "clave-incorrecta").exchange()
            .expectStatus().isUnauthorized().expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO");

        webTestClient.get().uri("/actuator/info").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isOk().expectHeader().exists("X-Trace-Id");
        webTestClient.get().uri("/actuator/prometheus").header("X-API-Key", "clave-prueba").exchange().expectStatus().isOk();
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
}
