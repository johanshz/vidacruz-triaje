package co.vidacruz.triage.reporte.configuracion;

import java.time.ZoneId;
import java.time.DateTimeException;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vidacruz.reporte")
public record PropiedadesReporte(String zonaHoraria) {
    public PropiedadesReporte {
        try {
            ZoneId.of(zonaHoraria);
        } catch (DateTimeException | NullPointerException exception) {
            throw new IllegalArgumentException("vidacruz.reporte.zona-horaria debe ser un ID de zona válido", exception);
        }
    }

    public ZoneId zona() {
        return ZoneId.of(zonaHoraria);
    }
}
