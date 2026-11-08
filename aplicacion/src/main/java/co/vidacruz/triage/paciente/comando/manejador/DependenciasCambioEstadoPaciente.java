package co.vidacruz.triage.paciente.comando.manejador;

import co.vidacruz.triage.paciente.puerto.dao.DaoPaciente;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import java.time.Clock;

public record DependenciasCambioEstadoPaciente(DaoPaciente dao, RepositorioPaciente pacientes,
        RegistrarCambioPaciente registrarCambio, EjecutorTransaccion transaccion, Clock reloj) { }
