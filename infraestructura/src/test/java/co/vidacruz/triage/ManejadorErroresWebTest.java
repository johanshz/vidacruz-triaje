package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import co.vidacruz.triage.infraestructura.error.FabricaProblemDetail;
import co.vidacruz.triage.infraestructura.error.ManejadorErroresWeb;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

class ManejadorErroresWebTest {

    private final ManejadorErroresWeb manejador = new ManejadorErroresWeb(
        new FabricaProblemDetail(new ObjectMapper()));

    @Test
    void traduce404405YErroresNoControlados() {
        verificarRespuesta(new ResponseStatusException(HttpStatus.NOT_FOUND, "recurso ausente"),
            HttpStatus.NOT_FOUND, "NO_ENCONTRADO");
        verificarRespuesta(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED),
            HttpStatus.METHOD_NOT_ALLOWED, "SOLICITUD_INVALIDA");
        verificarRespuesta(new ResponseStatusException(HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA");
        verificarRespuesta(new IllegalStateException("detalle privado"),
            HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO");
    }

    @Test
    void propagaErrorSiLaRespuestaYaSeEscribio() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        StepVerifier.create(exchange.getResponse().setComplete()).verifyComplete();
        IllegalStateException fallo = new IllegalStateException("respuesta cerrada");

        StepVerifier.create(manejador.handle(exchange, fallo))
            .expectErrorMatches(error -> error == fallo)
            .verify();
    }

    private void verificarRespuesta(Throwable error, HttpStatus status, String codigo) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        StepVerifier.create(manejador.handle(exchange, error)).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(status);
        StepVerifier.create(exchange.getResponse().getBodyAsString())
            .assertNext(cuerpo -> assertThat(cuerpo).contains("\"codigo\":\"" + codigo + "\""))
            .verifyComplete();
    }
}
