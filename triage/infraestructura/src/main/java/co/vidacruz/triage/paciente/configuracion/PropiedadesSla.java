package co.vidacruz.triage.paciente.configuracion;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vidacruz.sla.minutos")
public record PropiedadesSla(
        @Min(1) int critico,
        @Min(1) int urgente,
        @Min(1) int urgente2,
        @Min(1) int noUrgente,
        @Min(1) int noUrgente2) {
}
