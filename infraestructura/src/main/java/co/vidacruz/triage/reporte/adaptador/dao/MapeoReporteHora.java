package co.vidacruz.triage.reporte.adaptador.dao;

import co.vidacruz.triage.reporte.modelo.dto.DtoReporteHora;
import io.r2dbc.spi.Row;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

public class MapeoReporteHora {
    public DtoReporteHora mapear(Row fila, ZoneId zonaHoraria) {
        Instant hora = fila.get("hora", Instant.class);
        var porNivel = Map.of(
                "CRITICO", fila.get("critico", Long.class).intValue(),
                "URGENTE", fila.get("urgente", Long.class).intValue(),
                "URGENTE_2", fila.get("urgente2", Long.class).intValue(),
                "NO_URGENTE", fila.get("noUrgente", Long.class).intValue(),
                "NO_URGENTE_2", fila.get("noUrgente2", Long.class).intValue());
        return new DtoReporteHora(hora.atZone(zonaHoraria).toOffsetDateTime(),
                fila.get("total", Long.class).intValue(), fila.get("acumulado", Long.class).intValue(), porNivel,
                fila.get("promedio", BigDecimal.class), fila.get("p95", BigDecimal.class));
    }
}
