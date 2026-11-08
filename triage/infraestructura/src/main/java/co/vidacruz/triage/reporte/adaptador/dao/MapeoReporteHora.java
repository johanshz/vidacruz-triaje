package co.vidacruz.triage.reporte.adaptador.dao;

import co.vidacruz.triage.reporte.modelo.dto.DtoReporteHora;
import io.r2dbc.spi.Row;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;

public class MapeoReporteHora {
    private static final List<NivelReporte> NIVELES = List.of(
            new NivelReporte("CRITICO", "critico"),
            new NivelReporte("URGENTE", "urgente"),
            new NivelReporte("URGENTE_2", "urgente2"),
            new NivelReporte("NO_URGENTE", "noUrgente"),
            new NivelReporte("NO_URGENTE_2", "noUrgente2"));

    public DtoReporteHora mapear(Row fila, ZoneId zonaHoraria) {
        Instant hora = fila.get("hora", Instant.class);
        var porNivel = new LinkedHashMap<String, Integer>();
        for (NivelReporte nivel : NIVELES) {
            porNivel.put(nivel.llave(), fila.get(nivel.columna(), Long.class).intValue());
        }
        return new DtoReporteHora(hora.atZone(zonaHoraria).toOffsetDateTime(),
                fila.get("total", Long.class).intValue(), fila.get("acumulado", Long.class).intValue(), porNivel,
                fila.get("promedio", BigDecimal.class), fila.get("p95", BigDecimal.class));
    }

    private record NivelReporte(String llave, String columna) { }
}
