package co.vidacruz.triage.paciente.comando;

import co.vidacruz.triage.paciente.puerto.repositorio.ContadorClasificaciones;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import co.vidacruz.triage.paciente.servicio.ServicioClasificacionPaciente;
import java.time.Clock;

public record DependenciasRegistroPaciente(ServicioClasificacionPaciente clasificacion, FabricaPaciente fabrica,
        RepositorioPaciente pacientes, RegistrarCambioPaciente registrarCambio, EjecutorTransaccion transaccion,
        ContadorClasificaciones contador, Clock reloj) { }
