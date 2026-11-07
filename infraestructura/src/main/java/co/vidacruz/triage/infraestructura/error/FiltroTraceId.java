package co.vidacruz.triage.infraestructura.error;

import io.micrometer.tracing.Tracer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroTraceId implements WebFilter {

    public static final String ATRIBUTO_TRACE_ID = FiltroTraceId.class.getName() + ".traceId";
    private static final String CABECERA_TRACE_ID = "X-Trace-Id";
    private final Tracer tracer;

    public FiltroTraceId(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.deferContextual(contexto -> {
            Optional<String> traceId = contexto.<Observation>getOrEmpty(ObservationThreadLocalAccessor.KEY)
                .map(Observation::getContext)
                .<TracingContext>map(contextoObservacion -> contextoObservacion.get(TracingContext.class))
                .map(TracingContext::getSpan)
                .or(() -> Optional.ofNullable(tracer.currentSpan()))
                .map(span -> span.context().traceId());
            traceId.ifPresent(id -> exchange.getAttributes().put(ATRIBUTO_TRACE_ID, id));
            exchange.getResponse().beforeCommit(() -> {
                traceId.ifPresent(id -> exchange.getResponse().getHeaders().set(CABECERA_TRACE_ID, id));
                return Mono.empty();
            });
            return chain.filter(exchange);
        });
    }

}
