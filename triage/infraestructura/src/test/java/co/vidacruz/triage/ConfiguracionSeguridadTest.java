package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;

import co.vidacruz.triage.seguridad.ConfiguracionSeguridad;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ConfiguracionSeguridadTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ConfiguracionPrueba.class);

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @NullSource
    void apiKeyAusenteOVaciaFallaPorValidacion(String clave) {
        var runner = clave == null ? contextRunner : contextRunner.withPropertyValues("vidacruz.seguridad.api-key=" + clave);
        runner.run(context -> assertThat(context).hasFailed().getFailure()
            .hasRootCauseInstanceOf(BindValidationException.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConfiguracionSeguridad.class)
    static class ConfiguracionPrueba {
    }
}
