package co.vidacruz.triage.traza;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import java.util.Optional;
import org.springframework.stereotype.Component;
import reactor.util.context.ContextView;

@Component
public class LectorTraceparent {
    private static final String VERSION = "00";
    private static final String MUESTREADA = "01";
    private static final String NO_MUESTREADA = "00";
    private static final String TRACE_ID_VACIO = "00000000000000000000000000000000";

    private final Tracer tracer;

    public LectorTraceparent(Tracer tracer) {
        this.tracer = tracer;
    }

    public Optional<String> leer(ContextView contexto) {
        return contexto.<Observation>getOrEmpty(ObservationThreadLocalAccessor.KEY)
                .map(Observation::getContext)
                .<TracingContext>map(contextoObservacion -> contextoObservacion.get(TracingContext.class))
                .map(TracingContext::getSpan)
                .or(() -> Optional.ofNullable(tracer.currentSpan()))
                .map(Span::context)
                .filter(LectorTraceparent::esValido)
                .map(LectorTraceparent::formatear);
    }

    private static boolean esValido(TraceContext traza) {
        return traza.traceId() != null && traza.traceId().length() == 32 && !TRACE_ID_VACIO.equals(traza.traceId())
                && traza.spanId() != null && traza.spanId().length() == 16;
    }

    private static String formatear(TraceContext traza) {
        String banderas = Boolean.FALSE.equals(traza.sampled()) ? NO_MUESTREADA : MUESTREADA;
        return VERSION + "-" + traza.traceId() + "-" + traza.spanId() + "-" + banderas;
    }
}
