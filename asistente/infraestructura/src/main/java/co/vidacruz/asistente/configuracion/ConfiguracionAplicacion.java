package co.vidacruz.asistente.configuracion;

import co.vidacruz.asistente.urgencias.consulta.ManejadorConsultarEstadoUrgencias;
import co.vidacruz.asistente.protocolo.comando.fabrica.FabricaConsultaProtocolo;
import co.vidacruz.asistente.protocolo.comando.manejador.ManejadorConsultarProtocolo;
import co.vidacruz.asistente.protocolo.servicio.ServicioConsultarProtocolo;
import co.vidacruz.asistente.protocolo.configuracion.CondicionesProtocolo;
import co.vidacruz.asistente.urgencias.puerto.dao.DaoUrgencias;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
public class ConfiguracionAplicacion {
    @Bean
    ManejadorConsultarEstadoUrgencias manejadorConsultarEstadoUrgencias(DaoUrgencias dao) {
        return new ManejadorConsultarEstadoUrgencias(dao);
    }

    @Bean
    @ConditionalOnProperty(name = CondicionesProtocolo.PROPIEDAD_CARGAR_CORPUS, havingValue = "true", matchIfMissing = true)
    FabricaConsultaProtocolo fabricaConsultaProtocolo() {
        return new FabricaConsultaProtocolo();
    }

    @Bean
    @ConditionalOnProperty(name = CondicionesProtocolo.PROPIEDAD_CARGAR_CORPUS, havingValue = "true", matchIfMissing = true)
    ManejadorConsultarProtocolo manejadorConsultarProtocolo(FabricaConsultaProtocolo fabrica,
            ServicioConsultarProtocolo servicio) {
        return new ManejadorConsultarProtocolo(fabrica, servicio);
    }
}
