package co.vidacruz.triage.vigilancia.tarea;

import co.vidacruz.triage.vigilancia.comando.ComandoEscalarVencidos;
import co.vidacruz.triage.vigilancia.comando.ManejadorEscalarVencidos;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Component
public class TareaVigilanciaSla {
    private static final Logger LOGGER = LoggerFactory.getLogger(TareaVigilanciaSla.class);
    private final ManejadorEscalarVencidos manejador;
    private final AtomicBoolean ejecutando = new AtomicBoolean();

    public TareaVigilanciaSla(ManejadorEscalarVencidos manejador) {
        this.manejador = manejador;
    }

    @Scheduled(initialDelayString = "${vidacruz.vigilancia.retraso-inicial:PT0S}",
            fixedDelayString = "${vidacruz.vigilancia.intervalo:PT30S}")
    public Mono<Void> ejecutar() {
        return Mono.defer(() -> {
            if (!ejecutando.compareAndSet(false, true)) {
                return Mono.empty();
            }
            return manejador.ejecutar(new ComandoEscalarVencidos())
                    .doOnError(error -> LOGGER.error("Fallo en el ciclo de vigilancia SLA", error))
                    .onErrorResume(error -> Mono.empty())
                    .doFinally(this::liberarEjecucion);
        });
    }

    private void liberarEjecucion(SignalType senal) {
        ejecutando.set(false);
    }
}
