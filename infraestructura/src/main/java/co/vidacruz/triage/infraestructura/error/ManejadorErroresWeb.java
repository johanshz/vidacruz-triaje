package co.vidacruz.triage.infraestructura.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ManejadorErroresWeb implements WebExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErroresWeb.class);
    private final FabricaProblemDetail fabrica;
    private final TraductorErrorHttp traductor = new TraductorErrorHttp();

    public ManejadorErroresWeb(FabricaProblemDetail fabrica) { this.fabrica = fabrica; }

    @Override public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) return Mono.error(error);
        HttpStatusCode status = error instanceof ErrorResponse response ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR;
        var traduccion = traductor.traducir(status);
        if (status.is5xxServerError()) LOG.error("Error HTTP no controlado traceId={}", fabrica.traceId(exchange), error);
        return fabrica.escribir(exchange, fabrica.crear(exchange, traduccion.status(), traduccion.codigo(), traduccion.detalle()));
    }
}
