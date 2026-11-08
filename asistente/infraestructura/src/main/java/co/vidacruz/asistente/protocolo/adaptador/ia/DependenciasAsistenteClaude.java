package co.vidacruz.asistente.protocolo.adaptador.ia;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micrometer.core.instrument.MeterRegistry;

public record DependenciasAsistenteClaude(CircuitBreaker circuitBreaker,
        TimeLimiter timeLimiter, MeterRegistry meterRegistry) {
}
