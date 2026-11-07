package co.vidacruz.triage.paciente.configuracion;

import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.comando.RegistrarCambioPaciente;
import co.vidacruz.triage.paciente.comando.FabricaPaciente;
import co.vidacruz.triage.paciente.comando.DependenciasRegistroPaciente;
import co.vidacruz.triage.paciente.comando.DependenciasCambioEstadoPaciente;
import co.vidacruz.triage.paciente.comando.ManejadorCambiarEstadoPaciente;
import co.vidacruz.triage.paciente.comando.ManejadorRegistrarPaciente;
import co.vidacruz.triage.paciente.consulta.ManejadorConsultarPaciente;
import co.vidacruz.triage.paciente.puerto.dao.DaoPaciente;
import co.vidacruz.triage.paciente.puerto.repositorio.ContadorClasificaciones;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.auditoria.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.auditoria.modelo.entidad.RegistroAuditoria;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import co.vidacruz.triage.paciente.servicio.ServicioClasificacionPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.ConfiguracionSla;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionCasosUsoPaciente {
    @Bean ConfiguracionSla configuracionSla(PropiedadesSla propiedades) {
        return new ConfiguracionSla(propiedades.critico(), propiedades.urgente(), propiedades.urgente2(),
                propiedades.noUrgente(), propiedades.noUrgente2());
    }
    @Bean Clock relojTriage() { return Clock.systemUTC(); }
    @Bean ServicioClasificacionPaciente servicioClasificacionPaciente() { return new ServicioClasificacionPaciente(); }
    @Bean FabricaPaciente fabricaPaciente() { return new FabricaPaciente(); }
    @Bean RegistrarCambioPaciente registrarCambioPaciente(
            RepositorioAuditoria auditoria,
            RepositorioOutbox outbox) {
        return new RegistrarCambioPaciente(auditoria, outbox);
    }
    @Bean ManejadorRegistrarPaciente manejadorRegistrarPaciente(
            ServicioClasificacionPaciente servicio, FabricaPaciente fabrica,
            RepositorioPaciente pacientes, RegistrarCambioPaciente registrarCambio,
            EjecutorTransaccion transaccion, ContadorClasificaciones contador, Clock reloj,
            ConfiguracionSla configuracionSla) {
        return new ManejadorRegistrarPaciente(new DependenciasRegistroPaciente(servicio, fabrica, pacientes,
                registrarCambio, transaccion, contador, reloj, configuracionSla));
    }
    @Bean ManejadorConsultarPaciente manejadorConsultarPaciente(DaoPaciente dao, Clock reloj) {
        return new ManejadorConsultarPaciente(dao, reloj);
    }
    @Bean ManejadorCambiarEstadoPaciente manejadorCambiarEstadoPaciente(
            DaoPaciente dao, RepositorioPaciente pacientes,
            RegistrarCambioPaciente registrarCambio, EjecutorTransaccion transaccion, Clock reloj) {
        return new ManejadorCambiarEstadoPaciente(new DependenciasCambioEstadoPaciente(dao, pacientes,
                registrarCambio, transaccion, reloj));
    }
}
