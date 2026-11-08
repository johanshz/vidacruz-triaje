package co.vidacruz.triage.evento.puerto.repositorio;

import reactor.core.publisher.Mono;

public interface CandadoPublicacion {
    Mono<Boolean> tomar();
}
