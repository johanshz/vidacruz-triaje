package co.vidacruz.triage.paciente.puerto.repositorio;

import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import java.time.Instant;
import reactor.core.publisher.Mono;

public interface RepositorioAuditoria {
    Mono<Void> registrar(Paciente paciente, String tipoEvento, String estadoAnterior, String estadoNuevo, Instant ocurridoEn);
}
