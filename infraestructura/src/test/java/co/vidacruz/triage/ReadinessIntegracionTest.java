package co.vidacruz.triage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureObservability
class ReadinessIntegracionTest extends PruebaIntegracionPostgres {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void readinessCaeCuandoPostgresDejaDeResponder() {
        POSTGRES.stop();

        webTestClient.get().uri("/actuator/health/readiness").exchange()
            .expectStatus().isEqualTo(503)
            .expectBody().jsonPath("$.status").isEqualTo("DOWN");
    }
}
