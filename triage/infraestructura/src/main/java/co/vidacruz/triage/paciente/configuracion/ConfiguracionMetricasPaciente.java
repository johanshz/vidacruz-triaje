package co.vidacruz.triage.paciente.configuracion;

import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.puerto.repositorio.ContadorClasificaciones;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionMetricasPaciente {
    @Bean
    ContadorClasificaciones contadorClasificaciones(MeterRegistry registro) {
        return (NivelTriage nivel) -> io.micrometer.core.instrument.Counter.builder("triaje.clasificaciones")
                .tag("nivel", nivel.name()).register(registro).increment();
    }
}
