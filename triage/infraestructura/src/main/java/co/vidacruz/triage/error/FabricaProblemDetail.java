package co.vidacruz.triage.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class FabricaProblemDetail {

    public ProblemDetail crear(ServerWebExchange exchange, TraductorErrorHttp.ErrorHttp error) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(error.status(), error.detalle());
        HttpStatus estado = HttpStatus.resolve(error.status().value());
        problemDetail.setTitle(estado == null ? "Error HTTP" : estado.getReasonPhrase());
        problemDetail.setProperty("codigo", error.codigo().name());
        error.campo().ifPresent(campo -> problemDetail.setProperty("campo", campo));
        problemDetail.setProperty("traceId", exchange.getAttribute(FiltroTraceId.ATRIBUTO_TRACE_ID));
        return problemDetail;
    }
}
