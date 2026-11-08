package co.vidacruz.triage.paciente.comando.manejador;

import co.vidacruz.triage.paciente.modelo.entidad.ConfiguracionSla;
import co.vidacruz.triage.paciente.puerto.repositorio.ContadorClasificaciones;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import co.vidacruz.triage.paciente.servicio.ServicioClasificacionPaciente;
import java.time.Clock;

public record DependenciasRegistroPaciente(ServicioClasificacionPaciente clasificacion,
        RepositorioPaciente pacientes, RegistrarCambioPaciente registrarCambio, EjecutorTransaccion transaccion,
        ContextoRegistroPaciente contexto) {
    public record ContextoRegistroPaciente(ContadorClasificaciones contador, Clock reloj,
            ConfiguracionSla configuracionSla) { }
}
