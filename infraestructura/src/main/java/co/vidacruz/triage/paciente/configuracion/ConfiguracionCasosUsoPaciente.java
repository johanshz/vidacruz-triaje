package co.vidacruz.triage.paciente.configuracion;

import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.comando.FabricaPaciente;
import co.vidacruz.triage.paciente.comando.ManejadorCambiarEstadoPaciente;
import co.vidacruz.triage.paciente.comando.ManejadorRegistrarPaciente;
import co.vidacruz.triage.paciente.consulta.ManejadorConsultarPaciente;
import co.vidacruz.triage.paciente.puerto.dao.DaoPaciente;
import co.vidacruz.triage.paciente.puerto.repositorio.ContadorClasificaciones;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import co.vidacruz.triage.paciente.servicio.ServicioClasificacionPaciente;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionCasosUsoPaciente {
    @Bean Clock relojTriage() { return Clock.systemUTC(); }
    @Bean ServicioClasificacionPaciente servicioClasificacionPaciente() { return new ServicioClasificacionPaciente(); }
    @Bean FabricaPaciente fabricaPaciente() { return new FabricaPaciente(); }
    @Bean ManejadorRegistrarPaciente manejadorRegistrarPaciente(ServicioClasificacionPaciente servicio, FabricaPaciente fabrica,
            RepositorioPaciente pacientes, RepositorioAuditoria auditoria, RepositorioOutbox outbox,
            EjecutorTransaccion transaccion, ContadorClasificaciones contador, Clock reloj) {
        return new ManejadorRegistrarPaciente(servicio, fabrica, pacientes, auditoria, outbox, transaccion, contador, reloj);
    }
    @Bean ManejadorConsultarPaciente manejadorConsultarPaciente(DaoPaciente dao, Clock reloj) {
        return new ManejadorConsultarPaciente(dao, reloj);
    }
    @Bean ManejadorCambiarEstadoPaciente manejadorCambiarEstadoPaciente(DaoPaciente dao, RepositorioPaciente pacientes,
            RepositorioAuditoria auditoria, RepositorioOutbox outbox, EjecutorTransaccion transaccion, Clock reloj) {
        return new ManejadorCambiarEstadoPaciente(dao, pacientes, auditoria, outbox, transaccion, reloj);
    }
}
