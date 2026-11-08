package co.vidacruz.asistente.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ManejadorErroresWebFlux implements WebExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErroresWebFlux.class);
    private final ObjectMapper mapper;

    public ManejadorErroresWebFlux(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange intercambio, Throwable excepcion) {
        if (intercambio.getResponse().isCommitted()) return Mono.error(excepcion);
        if (!(excepcion instanceof ResponseStatusException respuesta)) return Mono.error(excepcion);

        HttpStatusCode estado = respuesta.getStatusCode();
        String codigo = estado.value() == 404 ? "NO_ENCONTRADO"
                : estado.is4xxClientError() ? "SOLICITUD_INVALIDA" : "ERROR_INTERNO";
        String detalle = estado.value() == 404 ? "La ruta solicitada no existe"
                : estado.is4xxClientError() ? "La solicitud no es válida" : "Ocurrió un error interno";
        if (estado.is5xxServerError()) LOG.error("Error no controlado al resolver la solicitud", excepcion);

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(HttpStatus.valueOf(estado.value()).getReasonPhrase());
        problema.setProperty("codigo", codigo);
        String traceId = intercambio.getResponse().getHeaders().getFirst("X-Trace-Id");
        problema.setProperty("traceId", traceId == null ? "desconocido" : traceId);
        intercambio.getResponse().setStatusCode(estado);
        intercambio.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        try {
            byte[] cuerpo = mapper.writeValueAsBytes(problema);
            return intercambio.getResponse().writeWith(Mono.just(intercambio.getResponse().bufferFactory().wrap(cuerpo)));
        } catch (Exception error) {
            return Mono.error(error);
        }
    }
}
