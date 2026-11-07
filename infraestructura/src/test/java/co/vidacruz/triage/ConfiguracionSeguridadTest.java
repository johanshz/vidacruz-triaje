package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;

import co.vidacruz.triage.seguridad.ConfiguracionSeguridad;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ConfiguracionSeguridadTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ConfiguracionPrueba.class);

    @Test
    void apiKeyVaciaImpideCrearLaConfiguracion() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableConfigurationProperties(ConfiguracionSeguridad.class)
    static class ConfiguracionPrueba {
    }
}
