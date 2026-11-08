package co.vidacruz.triage.alerta.puerto.repositorio;

import co.vidacruz.triage.alerta.modelo.dto.AlertaEscalamiento;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificadorAlerta {
    Mono<Void> notificar(AlertaEscalamiento alerta);
    Flux<AlertaEscalamiento> nuevas();
}
