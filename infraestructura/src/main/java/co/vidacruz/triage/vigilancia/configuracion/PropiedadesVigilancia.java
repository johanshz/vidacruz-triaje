package co.vidacruz.triage.vigilancia.configuracion;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vidacruz.vigilancia")
public record PropiedadesVigilancia(Duration intervalo) {
    public PropiedadesVigilancia {
        if (intervalo == null || intervalo.isZero() || intervalo.isNegative()) {
            throw new IllegalArgumentException("vidacruz.vigilancia.intervalo debe ser mayor que cero");
        }
    }
}
