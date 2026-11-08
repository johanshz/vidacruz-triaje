package co.vidacruz.asistente.protocolo;

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import co.vidacruz.asistente.Aplicacion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class ConfiguracionIaSinClaveTest {
    @Test
    void iniciaLaAplicacionConClaveAnthropicVacia() {
        try (ConfigurableApplicationContext contexto = new SpringApplicationBuilder(Aplicacion.class)
                .web(WebApplicationType.REACTIVE)
                .properties("vidacruz.seguridad.api-key=clave-test",
                        "spring.ai.anthropic.api-key=",
                        "vidacruz.ia.corpus.cargar=false",
                        "spring.kafka.listener.auto-startup=false",
                        "spring.kafka.bootstrap-servers=localhost:9092",
                        "spring.data.redis.host=localhost",
                        "server.port=0",
                        "logging.level.root=OFF")
                .run()) {
            assertNotNull(contexto.getBean(Aplicacion.class));
        }
    }
}
