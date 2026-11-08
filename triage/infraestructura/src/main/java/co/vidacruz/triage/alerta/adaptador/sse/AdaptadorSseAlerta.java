package co.vidacruz.triage.alerta.adaptador.sse;

import co.vidacruz.triage.alerta.modelo.dto.AlertaEscalamiento;
import co.vidacruz.triage.alerta.puerto.repositorio.NotificadorAlerta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.BufferOverflowStrategy;

@Component
public class AdaptadorSseAlerta implements NotificadorAlerta {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptadorSseAlerta.class);
    private final Sinks.Many<AlertaEscalamiento> sink;
    private final int capacidad;

    public AdaptadorSseAlerta(@Value("${vidacruz.alerta.buffer:256}") int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("vidacruz.alerta.buffer debe ser mayor que cero");
        }
        this.capacidad = capacidad;
        sink = Sinks.many().multicast().directBestEffort();
    }

    @Override
    public Flux<AlertaEscalamiento> nuevas() {
        return sink.asFlux().onBackpressureBuffer(capacidad,
                alerta -> LOGGER.warn("Se descartó una alerta del buffer SSE de suscriptor lento: {}",
                        alerta.idPaciente()), BufferOverflowStrategy.DROP_OLDEST);
    }

    @Override
    public Mono<Void> notificar(AlertaEscalamiento alerta) {
        return Mono.fromRunnable(() -> {
            Sinks.EmitResult resultado = sink.tryEmitNext(alerta);
            if (resultado == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
                return;
            }
            if (resultado == Sinks.EmitResult.FAIL_OVERFLOW || resultado == Sinks.EmitResult.FAIL_CANCELLED) {
                return;
            }
            if (resultado.isFailure()) {
                throw new IllegalStateException("No fue posible emitir la alerta SSE: " + resultado);
            }
        });
    }
}
