package co.vidacruz.triage.paciente.comando;

import co.vidacruz.triage.auditoria.modelo.entidad.RegistroAuditoria;
import co.vidacruz.triage.auditoria.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import reactor.core.publisher.Mono;

public class RegistrarCambioPaciente {
    private final RepositorioAuditoria auditoria;
    private final RepositorioOutbox outbox;

    public RegistrarCambioPaciente(RepositorioAuditoria auditoria, RepositorioOutbox outbox) {
        this.auditoria = auditoria;
        this.outbox = outbox;
    }

    public Mono<Void> registrar(Paciente paciente, EventoPaciente evento, EstadoPaciente estadoAnterior) {
        String anterior = estadoAnterior == null ? null : estadoAnterior.name();
        return auditoria.registrar(new RegistroAuditoria(paciente.id(), evento.tipo(), anterior,
                paciente.estado().name(), evento.ocurridoEn()))
                .then(outbox.agregar(evento, paciente.id().toString()));
    }
}
