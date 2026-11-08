package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;
import co.vidacruz.triage.paciente.configuracion.PropiedadesSla;
import co.vidacruz.triage.reporte.configuracion.PropiedadesReporte;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PropiedadesSlaTest {
    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ConfiguracionPropiedades.class);

    @Test
    void valorSlaCeroImpideCrearElContexto() {
        contexto.withPropertyValues("vidacruz.sla.minutos.critico=0").run(
                resultado -> assertThat(resultado).hasFailed());
    }

    @Test
    void textoEnSlaImpideCrearElContexto() {
        contexto.withPropertyValues("vidacruz.sla.minutos.urgente=no-es-entero").run(
                resultado -> assertThat(resultado).hasFailed());
    }

    @Test
    void identificadorDeZonaInvalidoImpideCrearElContexto() {
        contexto.withPropertyValues("vidacruz.reporte.zona-horaria=Zona/Inexistente").run(
                resultado -> assertThat(resultado).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({PropiedadesSla.class, PropiedadesReporte.class})
    static class ConfiguracionPropiedades { }
}
