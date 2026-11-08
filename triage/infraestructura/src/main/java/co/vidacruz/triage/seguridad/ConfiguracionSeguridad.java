package co.vidacruz.triage.seguridad;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vidacruz.seguridad")
public record ConfiguracionSeguridad(@NotBlank String apiKey) {
}
