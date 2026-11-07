package co.vidacruz.asistente.urgencias.consulta;

import co.vidacruz.asistente.urgencias.modelo.dto.DtoEstadoUrgencias;
import co.vidacruz.asistente.urgencias.puerto.dao.DaoUrgencias;
import reactor.core.publisher.Mono;

public final class ManejadorConsultarEstadoUrgencias {
    private final DaoUrgencias dao;

    public ManejadorConsultarEstadoUrgencias(DaoUrgencias dao) {
        this.dao = dao;
    }

    public Mono<DtoEstadoUrgencias> manejar() {
        return dao.consultarEstado();
    }
}
