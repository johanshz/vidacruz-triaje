package co.vidacruz.asistente.configuracion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vidacruz.ia")
public record PropiedadesIa(@NotBlank String modelo,
        @DecimalMin("0.0") @DecimalMax("1.0") double umbralSimilitud,
        @Positive int topK, @Valid Resiliencia resiliencia) {

    public record Resiliencia(Duration timeout, Duration timeoutEmbeddings, @Positive int ventana,
            @Positive int minimoLlamadas, @DecimalMin("1.0") @DecimalMax("100.0") float porcentajeFallos,
            Duration esperaAbierto, @Positive int llamadasSemiabierto) {
    }
}
