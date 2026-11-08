package co.vidacruz.triage.reporte.configuracion;

import co.vidacruz.triage.reporte.consulta.ManejadorConsultarReporteOperacion;
import co.vidacruz.triage.reporte.puerto.dao.DaoReporteOperacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionReporte {
    @Bean
    ManejadorConsultarReporteOperacion manejadorConsultarReporteOperacion(DaoReporteOperacion dao,
            PropiedadesReporte propiedades) {
        return new ManejadorConsultarReporteOperacion(dao, propiedades.zona());
    }
}
