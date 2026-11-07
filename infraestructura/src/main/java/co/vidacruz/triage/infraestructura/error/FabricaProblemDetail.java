package co.vidacruz.triage.infraestructura.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class FabricaProblemDetail {

    private final ObjectMapper objectMapper;

    public FabricaProblemDetail(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProblemDetail crear(ServerWebExchange exchange, HttpStatusCode status, String codigo,
                               String detalle, String campo) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detalle);
        HttpStatus estado = HttpStatus.resolve(status.value());
        problemDetail.setTitle(estado == null ? "Error HTTP" : estado.getReasonPhrase());
        problemDetail.setProperty("codigo", codigo);
        if (status.value() == 400) {
            problemDetail.setProperty("campo", campo);
        }
        problemDetail.setProperty("traceId", traceId(exchange));
        return problemDetail;
    }

    public ProblemDetail crear(ServerWebExchange exchange, HttpStatusCode status, String codigo, String detalle) {
        return crear(exchange, status, codigo, detalle, null);
    }

    public Mono<Void> escribir(ServerWebExchange exchange, ProblemDetail problemDetail) {
        try {
            byte[] cuerpo = objectMapper.writeValueAsBytes(problemDetail);
            exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(problemDetail.getStatus()));
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(cuerpo)));
        } catch (JsonProcessingException error) {
            return Mono.error(error);
        }
    }

    public String traceId(ServerWebExchange exchange) {
        String traceId = exchange.getAttribute(FiltroTraceId.ATRIBUTO_TRACE_ID);
        if (traceId == null) {
            traceId = FiltroTraceId.nuevoTraceId();
            exchange.getAttributes().put(FiltroTraceId.ATRIBUTO_TRACE_ID, traceId);
        }
        return traceId;
    }
}
