package co.vidacruz.triage.vigilancia.modelo.entidad;

import co.vidacruz.triage.auditoria.modelo.entidad.RegistroAuditoria;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;

public record CambioEscalamiento(RegistroAuditoria auditoria, EventoPaciente evento) { }
