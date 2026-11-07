package co.vidacruz.triage.vigilancia.servicio;

import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.vigilancia.modelo.entidad.PacienteEscalado;
import co.vidacruz.triage.auditoria.modelo.entidad.RegistroAuditoria;
import co.vidacruz.triage.evento.modelo.entidad.DatosEventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.TipoEventoPaciente;
import co.vidacruz.triage.vigilancia.modelo.entidad.CambioEscalamiento;
import java.time.Instant;
import java.util.UUID;

public class ServicioEscalamiento {
    public boolean estaVencido(Paciente paciente, Instant ahora) {
        return paciente.estado() == EstadoPaciente.REGISTRADO && !paciente.escalado()
                && !ahora.isBefore(paciente.fechaRegistro().plusSeconds(paciente.slaMinutos() * 60L));
    }

    public CambioEscalamiento crearCambio(PacienteEscalado paciente, Instant ocurridoEn) {
        String tipo = TipoEventoPaciente.PACIENTE_ESCALADO.valor();
        String estado = EstadoPaciente.REGISTRADO.name();
        var auditoria = new RegistroAuditoria(paciente.id(), tipo, estado, estado, ocurridoEn);
        var evento = new EventoPaciente(UUID.randomUUID(), tipo, EventoPaciente.VERSION_ACTUAL, ocurridoEn,
                new DatosEventoPaciente(paciente.id(), paciente.nivel().name(), estado, estado, true));
        return new CambioEscalamiento(auditoria, evento);
    }
}
