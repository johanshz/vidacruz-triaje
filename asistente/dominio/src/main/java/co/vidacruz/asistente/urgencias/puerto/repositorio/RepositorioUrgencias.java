package co.vidacruz.asistente.urgencias.puerto.repositorio;

import co.vidacruz.asistente.urgencias.modelo.entidad.NivelUrgencias;
import reactor.core.publisher.Mono;

public interface RepositorioUrgencias {
    Mono<Void> agregar(NivelUrgencias nivel, String idPaciente);
    Mono<Void> quitarDeTodosLosNiveles(String idPaciente);
}
