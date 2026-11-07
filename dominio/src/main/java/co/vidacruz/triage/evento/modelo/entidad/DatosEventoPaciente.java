package co.vidacruz.triage.evento.modelo.entidad;

import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import java.util.UUID;

public record DatosEventoPaciente(UUID idPaciente, NivelTriage nivel, EstadoPaciente estadoAnterior,
        EstadoPaciente estadoNuevo, boolean escalado) { }
