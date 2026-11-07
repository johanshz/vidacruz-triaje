package co.vidacruz.asistente.configuracion;

import co.vidacruz.asistente.urgencias.puerto.repositorio.RepositorioUrgencias;
import co.vidacruz.asistente.urgencias.servicio.ServicioActualizarUrgencias;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionDominio {
    @Bean
    ServicioActualizarUrgencias servicioActualizarUrgencias(RepositorioUrgencias repositorio) {
        return new ServicioActualizarUrgencias(repositorio);
    }
}
