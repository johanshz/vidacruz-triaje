package co.vidacruz.triage.evento.puerto.repositorio;

import co.vidacruz.triage.evento.modelo.entidad.EventoOutbox;
import reactor.core.publisher.Mono;

public interface PublicadorEventos {
    Mono<Void> publicar(EventoOutbox evento);
}
