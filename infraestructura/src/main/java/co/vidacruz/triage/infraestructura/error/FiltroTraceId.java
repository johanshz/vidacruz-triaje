package co.vidacruz.triage.infraestructura.error;

import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public static final String ATRIBUTO_TRACE_ID = FiltroTraceId.class.getName() + ".traceId";
    private static final String CABECERA_TRACE_ID = "X-Trace-Id";
    private static final Logger LOG = LoggerFactory.getLogger(FiltroTraceId.class);
    private final Tracer tracer;

    public FiltroTraceId(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.deferContextual(contexto -> {
            String traceId = obtenerTraceId();
            exchange.getAttributes().put(ATRIBUTO_TRACE_ID, traceId);
            exchange.getResponse().beforeCommit(() -> {
                exchange.getResponse().getHeaders().set(CABECERA_TRACE_ID, traceId);
                return Mono.empty();
            });
            return chain.filter(exchange);
        });
    }

    private String obtenerTraceId() {
        var span = tracer.currentSpan();
        if (span != null) return span.context().traceId();
        LOG.warn("No hay span actual; se genera un traceId de respaldo");
        return nuevoTraceId();
    }

    static String nuevoTraceId() { return UUID.randomUUID().toString().replace("-", ""); }
}
