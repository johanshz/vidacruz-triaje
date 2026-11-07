package co.vidacruz.triage.paciente.puerto.repositorio;

import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface RepositorioPaciente {
    Mono<Void> guardar(Paciente paciente);
    Mono<Boolean> cambiarEstado(UUID id, Paciente paciente, String estadoAnterior);
}
