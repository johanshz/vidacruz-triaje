package co.vidacruz.asistente.urgencias.puerto.dao;

import co.vidacruz.asistente.urgencias.modelo.dto.DtoEstadoUrgencias;
import reactor.core.publisher.Mono;

public interface DaoUrgencias {
    Mono<DtoEstadoUrgencias> consultarEstado();
}
