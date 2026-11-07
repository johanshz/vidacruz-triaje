package co.vidacruz.asistente;

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class ConfiguracionSeguridadTest {
    @Test
    void noIniciaCuandoLaApiKeyEstaAusente() {
        Exception error = Assertions.assertThrows(Exception.class, () ->
                new SpringApplicationBuilder(Aplicacion.class)
                        .web(WebApplicationType.REACTIVE)
                        .properties("vidacruz.seguridad.api-key=", "server.port=0", "logging.level.root=OFF")
                        .run());

        Throwable causaRaiz = error;
        while (causaRaiz.getCause() != null) {
            causaRaiz = causaRaiz.getCause();
        }
        assertTrue(causaRaiz.getMessage().contains("apiKey"));
    }
}
