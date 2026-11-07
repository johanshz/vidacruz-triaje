package co.vidacruz.triage.paciente.comando;

import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import java.util.UUID;

public record ComandoCambiarEstadoPaciente(UUID id, EstadoPaciente estado) { }
