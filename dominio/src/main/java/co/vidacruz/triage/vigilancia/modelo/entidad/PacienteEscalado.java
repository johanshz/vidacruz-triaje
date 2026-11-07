package co.vidacruz.triage.vigilancia.modelo.entidad;

import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import java.util.UUID;

public record PacienteEscalado(UUID id, NivelTriage nivel) { }
