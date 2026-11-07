package co.vidacruz.triage.auditoria.modelo.entidad;

import java.time.Instant;
import java.util.UUID;

public record RegistroAuditoria(UUID pacienteId, String tipoEvento, String estadoAnterior,
        String estadoNuevo, Instant ocurridoEn) { }
