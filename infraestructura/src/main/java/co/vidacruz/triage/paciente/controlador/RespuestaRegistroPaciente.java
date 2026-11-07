package co.vidacruz.triage.paciente.controlador;

import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import java.time.Instant;
import java.util.UUID;

public record RespuestaRegistroPaciente(UUID id, String nivel, int slaMinutos, String estado, Instant fechaRegistro) {
    public static RespuestaRegistroPaciente desde(Paciente paciente) {
        return new RespuestaRegistroPaciente(paciente.id(), paciente.nivel().name(), paciente.nivel().slaMinutos(),
                paciente.estado().name(), paciente.fechaRegistro());
    }
}
