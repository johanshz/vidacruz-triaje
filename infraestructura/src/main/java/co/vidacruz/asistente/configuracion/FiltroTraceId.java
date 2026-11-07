package co.vidacruz.asistente.configuracion;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroTraceId implements WebFilter {
    private static final Logger LOG = LoggerFactory.getLogger(FiltroTraceId.class);
    private final Tracer tracer;

    public FiltroTraceId(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange intercambio, WebFilterChain cadena) {
        Span span = tracer.currentSpan();
        String traceId = span == null ? UUID.randomUUID().toString().replace("-", "") : span.context().traceId();
        intercambio.getResponse().getHeaders().set("X-Trace-Id", traceId);
        intercambio.getAttributes().put("traceId", traceId);
        LOG.info("Solicitud HTTP recibida: method={}, path={}, traceId={}",
                intercambio.getRequest().getMethod(), intercambio.getRequest().getPath(), traceId);
        return cadena.filter(intercambio);
    }
}
