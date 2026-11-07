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
    private final EscritorProblemDetail escritor;
    private final TraductorErrorHttp traductor;

    public ManejadorErroresWeb(FabricaProblemDetail fabrica, EscritorProblemDetail escritor, TraductorErrorHttp traductor) {
        this.fabrica = fabrica;
        this.escritor = escritor;
        this.traductor = traductor;
    }

    @Override public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) return Mono.error(error);
        HttpStatusCode status = error instanceof ErrorResponse response ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR;
        var traduccion = traductor.traducir(status);
        if (status.is5xxServerError()) LOG.error("Error HTTP no controlado traceId={}", exchange.getAttribute(FiltroTraceId.ATRIBUTO_TRACE_ID), error);
        return escritor.escribir(exchange, fabrica.crear(exchange, traduccion));
    }
}
