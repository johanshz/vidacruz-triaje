package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class ArranqueIntegracionTest {

    @Test
    void flywayImpideArrancarCuandoPostgresNoResponde() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(Aplicacion.class)
            .web(WebApplicationType.NONE)
            .properties(Map.of(
                "SERVER_PORT", "8080",
                "DB_HOST", "127.0.0.1",
                "DB_PORT", "1",
                "DB_NAME", "inexistente",
                "DB_USER", "usuario-prueba",
                "DB_PASSWORD", "clave-prueba",
                "TRIAGE_API_KEY", "clave-prueba",
                "spring.flyway.connect-retries", "0",
                "spring.main.banner-mode", "off"
            ))
            .run())
            .isInstanceOf(Exception.class);
    }
}
