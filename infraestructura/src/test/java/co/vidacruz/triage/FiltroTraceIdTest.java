package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.vidacruz.triage.error.FiltroTraceId;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class FiltroTraceIdTest {
    @Test
    void cabeceraUsaElTraceIdDelSpanDisponibleAlSuscribirse() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-span-123");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        var filtro = new FiltroTraceId(tracer);

        StepVerifier.create(filtro.filter(exchange, actual -> actual.getResponse().setComplete())).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id")).isEqualTo("trace-span-123");
    }
}
