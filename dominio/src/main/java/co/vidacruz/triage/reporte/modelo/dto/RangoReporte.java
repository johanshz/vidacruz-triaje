package co.vidacruz.triage.reporte.modelo.dto;

import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public record RangoReporte(OffsetDateTime desde, OffsetDateTime hasta) {
    private static final Duration RANGO_MAXIMO = Duration.ofDays(31);

    public static RangoReporte desdeParametros(String desdeTexto, String hastaTexto) {
        OffsetDateTime desde = parsear(desdeTexto, "desde");
        OffsetDateTime hasta = parsear(hastaTexto, "hasta");
        if (!desde.isBefore(hasta)) {
            throw new ExcepcionValidacion("hasta", "Debe ser posterior a desde");
        }
        if (Duration.between(desde.toInstant(), hasta.toInstant()).compareTo(RANGO_MAXIMO) > 0) {
            throw new ExcepcionValidacion("hasta", "El rango no puede superar 31 días");
        }
        return new RangoReporte(desde, hasta);
    }

    private static OffsetDateTime parsear(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ExcepcionValidacion(campo, "El parámetro es obligatorio");
        }
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException exception) {
            throw new ExcepcionValidacion(campo, "Debe ser una fecha ISO 8601 con Z u offset");
        }
    }
}
