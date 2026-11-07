package co.vidacruz.triage.paciente.modelo.entidad;

import java.time.Instant;
import java.util.UUID;

public record Paciente(UUID id, DatosRegistroPaciente datos, NivelTriage nivel, EstadoPaciente estado,
        boolean escalado, Instant fechaRegistro, Instant fechaEstado, Instant fechaPrimeraConsulta) {
    public Paciente cambiarEstado(EstadoPaciente nuevoEstado, Instant ahora) {
        estado.validarTransicion(nuevoEstado);
        Instant primeraConsulta = nuevoEstado == EstadoPaciente.EN_CONSULTA && fechaPrimeraConsulta == null ? ahora : fechaPrimeraConsulta;
        return new Paciente(id, datos, nivel, nuevoEstado, escalado, fechaRegistro, ahora, primeraConsulta);
    }
}
