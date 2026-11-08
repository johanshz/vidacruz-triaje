package co.vidacruz.asistente.urgencias.comando.manejador;

import co.vidacruz.asistente.urgencias.comando.ComandoActualizarUrgencias;
import co.vidacruz.asistente.urgencias.servicio.ServicioActualizarUrgencias;
import reactor.core.publisher.Mono;

public final class ManejadorActualizarUrgencias {
    private final ServicioActualizarUrgencias servicio;

    public ManejadorActualizarUrgencias(ServicioActualizarUrgencias servicio) {
        this.servicio = servicio;
    }

    public Mono<Void> manejar(ComandoActualizarUrgencias comando) {
        return servicio.actualizar(comando.tipo(), comando.idPaciente(), comando.nivel());
    }
}
