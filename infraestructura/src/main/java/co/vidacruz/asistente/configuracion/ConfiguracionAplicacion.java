package co.vidacruz.asistente.configuracion;

import co.vidacruz.asistente.urgencias.consulta.ManejadorConsultarEstadoUrgencias;
import co.vidacruz.asistente.urgencias.puerto.dao.DaoUrgencias;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionAplicacion {
    @Bean
    ManejadorConsultarEstadoUrgencias manejadorConsultarEstadoUrgencias(DaoUrgencias dao) {
        return new ManejadorConsultarEstadoUrgencias(dao);
    }
}
