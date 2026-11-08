package co.vidacruz.triage.evento.configuracion;

import co.vidacruz.triage.evento.puerto.repositorio.ContadorEventosPendientes;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionMetricasOutbox {
    @Bean
    ContadorEventosPendientes contadorEventosPendientes(MeterRegistry registro) {
        AtomicLong pendientes = new AtomicLong();
        registro.gauge("triaje.outbox.pendientes", pendientes);
        return pendientes::set;
    }
}
