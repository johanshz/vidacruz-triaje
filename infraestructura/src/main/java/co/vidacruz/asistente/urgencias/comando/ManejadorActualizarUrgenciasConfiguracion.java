package co.vidacruz.asistente.configuracion;

import co.vidacruz.asistente.urgencias.comando.ManejadorActualizarUrgencias;
import co.vidacruz.asistente.urgencias.servicio.ServicioActualizarUrgencias;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManejadorActualizarUrgenciasConfiguracion {
    @Bean
    ManejadorActualizarUrgencias manejadorActualizarUrgencias(ServicioActualizarUrgencias servicio) {
        return new ManejadorActualizarUrgencias(servicio);
    }
}
