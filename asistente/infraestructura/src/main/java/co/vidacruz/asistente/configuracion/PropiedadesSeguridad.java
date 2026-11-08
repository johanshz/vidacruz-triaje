package co.vidacruz.asistente.configuracion;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vidacruz.seguridad")
public record PropiedadesSeguridad(@NotBlank String apiKey) {
}
