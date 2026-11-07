package co.vidacruz.triage.paciente.comando;

import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import java.time.Instant;
import java.util.UUID;

public record EntradaPacienteRegistrado(DatosRegistroPaciente datos, NivelTriage nivel, int slaMinutos,
        UUID id, Instant ahora) { }
