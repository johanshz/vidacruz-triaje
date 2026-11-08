package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
class ConfiguracionAplicacionTest {
    @Test
    void configuracionEliminaDbUrlYActivaTrazasEcs() throws IOException {
        try (var recurso = getClass().getResourceAsStream("/application.yaml")) {
            assertTrue(recurso != null);
            String configuracion = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(configuracion.contains("${DB_HOST}"));
            assertTrue(configuracion.contains("${DB_NAME}"));
            assertFalse(configuracion.contains("DB_URL"));
            assertTrue(configuracion.contains("console: ecs"));
            assertTrue(configuracion.contains("context-propagation: auto"));
        }
    }
}
