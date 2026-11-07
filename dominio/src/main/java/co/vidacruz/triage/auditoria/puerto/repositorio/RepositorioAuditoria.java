package co.vidacruz.triage.auditoria.puerto.repositorio;

import co.vidacruz.triage.auditoria.modelo.entidad.RegistroAuditoria;
import reactor.core.publisher.Mono;

public interface RepositorioAuditoria {
    Mono<Void> registrar(RegistroAuditoria registro);
}
