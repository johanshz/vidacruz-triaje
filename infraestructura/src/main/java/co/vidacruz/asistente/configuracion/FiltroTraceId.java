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
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroTraceId implements WebFilter {
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
        return cadena.filter(intercambio);
    }
}
