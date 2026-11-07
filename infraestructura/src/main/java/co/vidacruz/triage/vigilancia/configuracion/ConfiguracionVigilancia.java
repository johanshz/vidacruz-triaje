package co.vidacruz.triage.vigilancia.configuracion;

import co.vidacruz.triage.auditoria.puerto.repositorio.RepositorioAuditoria;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.paciente.puerto.repositorio.EjecutorTransaccion;
import co.vidacruz.triage.vigilancia.comando.DependenciasEscalamiento;
import co.vidacruz.triage.vigilancia.comando.ManejadorEscalarVencidos;
import co.vidacruz.triage.vigilancia.puerto.repositorio.ContadorEscalamientos;
import co.vidacruz.triage.vigilancia.puerto.repositorio.RepositorioEscalamiento;
import co.vidacruz.triage.vigilancia.servicio.ServicioEscalamiento;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionVigilancia {
    @Bean
    ServicioEscalamiento servicioEscalamiento() {
        return new ServicioEscalamiento();
    }

    @Bean
    ContadorEscalamientos contadorEscalamientos(MeterRegistry registro) {
        var contador = io.micrometer.core.instrument.Counter.builder("triaje.escalamientos").register(registro);
        return contador::increment;
    }

    @Bean
    ManejadorEscalarVencidos manejadorEscalarVencidos(RepositorioEscalamiento repositorio,
            RepositorioAuditoria auditoria, RepositorioOutbox outbox, EjecutorTransaccion transaccion,
            ServicioEscalamiento servicio, ContadorEscalamientos contador, Clock reloj) {
        return new ManejadorEscalarVencidos(new DependenciasEscalamiento(repositorio, auditoria, outbox,
                transaccion, servicio, contador, reloj));
    }
}
