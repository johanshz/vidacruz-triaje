package co.vidacruz.triage.reporte.configuracion;

import java.time.ZoneId;
import java.time.DateTimeException;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vidacruz.reporte")
public record PropiedadesReporte(String zonaHoraria) {
    public PropiedadesReporte {
        if (zonaHoraria == null || zonaHoraria.isBlank()) {
            throw new IllegalArgumentException("vidacruz.reporte.zona-horaria debe ser un ID de zona válido");
        }
        try {
            ZoneId.of(zonaHoraria);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("vidacruz.reporte.zona-horaria debe ser un ID de zona válido", exception);
        }
    }

    public ZoneId zona() {
        return ZoneId.of(zonaHoraria);
    }
}
