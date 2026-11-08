package co.vidacruz.asistente.urgencias.configuracion;

import co.vidacruz.asistente.urgencias.comando.manejador.ManejadorActualizarUrgencias;
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
