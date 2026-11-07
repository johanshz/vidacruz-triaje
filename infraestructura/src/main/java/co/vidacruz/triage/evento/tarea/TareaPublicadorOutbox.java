package co.vidacruz.triage.evento.tarea;

import co.vidacruz.triage.evento.comando.ManejadorPublicarOutbox;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TareaPublicadorOutbox {
    private final ManejadorPublicarOutbox manejador;
    public TareaPublicadorOutbox(ManejadorPublicarOutbox manejador) { this.manejador = manejador; }

    @Scheduled(fixedDelayString = "${vidacruz.outbox.intervalo:PT1S}",
            initialDelayString = "${vidacruz.outbox.retraso-inicial:PT0S}")
    public Mono<Void> publicar() {
        return manejador.ejecutar();
    }
}
