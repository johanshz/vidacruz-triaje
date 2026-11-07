package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureObservability
@Import(ControladorErroresPrueba.class)
@ExtendWith(OutputCaptureExtension.class)
class ErroresHttpIntegracionTest extends SoporteIntegracionTriage {
    @Test
    void erroresDeDominioRutaInexistenteMetodoNoPermitidoYTipoNoSoportadoSeSerializanComoProblemDetail() throws IOException {
        webTestClient.get().uri("/pruebas/error/validacion").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isBadRequest().expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION")
            .jsonPath("$.campo").isEqualTo("edad").jsonPath("$.traceId").exists();

        webTestClient.get().uri("/pruebas/error/no-encontrado").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isNotFound().expectBody().jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO");

        webTestClient.get().uri("/pruebas/error/conflicto").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isEqualTo(409).expectBody().jsonPath("$.codigo").isEqualTo("TRANSICION_INVALIDA");

        var error404 = webTestClient.get().uri("/ruta-inexistente").header("X-API-Key", "clave-prueba").exchange()
            .expectStatus().isNotFound().expectBody().jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO")
            .jsonPath("$.traceId").exists().returnResult();
        assertEquals(error404.getResponseHeaders().getFirst("X-Trace-Id"),
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(error404.getResponseBody()).get("traceId").asText());

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
    void cabeceraYLogEcsCompartenElTraceIdDelSpan(CapturedOutput salida) throws IOException {
        var resultado = webTestClient.get().uri("/pruebas/error/interno").header("X-API-Key", "clave-prueba")
            .exchange().expectStatus().is5xxServerError().expectBody().returnResult();
        String traceIdCabecera = resultado.getResponseHeaders().getFirst("X-Trace-Id");
        String traceIdCuerpo = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(resultado.getResponseBody()).get("traceId").asText();

        assertTrue(traceIdCabecera != null && !traceIdCabecera.isBlank());
        assertEquals(traceIdCabecera, traceIdCuerpo);
        assertTrue(salida.getOut().contains("\"traceId\":\"" + traceIdCabecera + "\""));
    }
}
