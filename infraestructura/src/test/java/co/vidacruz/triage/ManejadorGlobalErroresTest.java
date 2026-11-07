package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;

import co.vidacruz.triage.infraestructura.error.FabricaProblemDetail;
import co.vidacruz.triage.infraestructura.error.ManejadorGlobalErrores;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

class ManejadorGlobalErroresTest {

    private final ManejadorGlobalErrores manejador = new ManejadorGlobalErrores(
        new FabricaProblemDetail(), new co.vidacruz.triage.infraestructura.error.TraductorErrorHttp());

    @Test
    void traduceErroresHttpAProblemDetail() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        var error405 = manejador.errorHttp(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED), exchange);
        assertThat(error405.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(error405.getBody().getProperties()).containsEntry("codigo", "SOLICITUD_INVALIDA");

        var error503 = manejador.errorHttp(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE), exchange);
        assertThat(error503.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(error503.getBody().getProperties()).containsEntry("codigo", "ERROR_INTERNO");

        var recurso = manejador.recursoHttpNoEncontrado(
            new NoResourceFoundException("GET /ruta"), exchange);
        assertThat(recurso.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(recurso.getBody().getProperties()).containsEntry("codigo", "NO_ENCONTRADO");
    }
}
