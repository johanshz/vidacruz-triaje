package co.vidacruz.asistente.configuracion;

import co.vidacruz.asistente.urgencias.puerto.repositorio.RepositorioUrgencias;
import co.vidacruz.asistente.urgencias.servicio.ServicioActualizarUrgencias;
import co.vidacruz.asistente.protocolo.puerto.dao.AsistenteProtocolos;
import co.vidacruz.asistente.protocolo.puerto.dao.BuscadorProtocolos;
import co.vidacruz.asistente.protocolo.servicio.ServicioConsultarProtocolo;
import co.vidacruz.asistente.protocolo.servicio.ParametrosBusqueda;
import co.vidacruz.asistente.protocolo.configuracion.CondicionesProtocolo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
public class ConfiguracionDominio {
    @Bean
    ServicioActualizarUrgencias servicioActualizarUrgencias(RepositorioUrgencias repositorio) {
        return new ServicioActualizarUrgencias(repositorio);
    }

    @Bean
    @ConditionalOnProperty(name = CondicionesProtocolo.PROPIEDAD_CARGAR_CORPUS, havingValue = "true", matchIfMissing = true)
    ServicioConsultarProtocolo servicioConsultarProtocolo(BuscadorProtocolos buscador,
            AsistenteProtocolos asistente, PropiedadesIa propiedades) {
        return new ServicioConsultarProtocolo(buscador, asistente,
                new ParametrosBusqueda(propiedades.umbralSimilitud(), propiedades.topK()));
    }
}
