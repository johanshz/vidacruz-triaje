package co.vidacruz.triage.reporte.adaptador.dao;

import co.vidacruz.triage.reporte.modelo.dto.DtoReporteHora;
import co.vidacruz.triage.reporte.modelo.dto.RangoReporte;
import co.vidacruz.triage.reporte.puerto.dao.DaoReporteOperacion;
import java.time.ZoneId;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public class DaoReporteOperacionPostgres implements DaoReporteOperacion {
    private static final String CONSULTA = """
            WITH agregados AS (
                SELECT date_trunc('hour', p.fecha_registro AT TIME ZONE :zona) AS hora_local,
                    COUNT(*) AS total,
                    COUNT(*) FILTER (WHERE p.nivel = 'CRITICO') AS critico,
                    COUNT(*) FILTER (WHERE p.nivel = 'URGENTE') AS urgente,
                    COUNT(*) FILTER (WHERE p.nivel = 'URGENTE_2') AS urgente2,
                    COUNT(*) FILTER (WHERE p.nivel = 'NO_URGENTE') AS noUrgente,
                    COUNT(*) FILTER (WHERE p.nivel = 'NO_URGENTE_2') AS noUrgente2,
                    ROUND((AVG(EXTRACT(EPOCH FROM (p.fecha_primera_consulta - p.fecha_registro)) / 60)
                        FILTER (WHERE p.fecha_primera_consulta IS NOT NULL))::numeric, 1) AS promedio,
                    ROUND((percentile_cont(0.95) WITHIN GROUP (ORDER BY
                        EXTRACT(EPOCH FROM (p.fecha_primera_consulta - p.fecha_registro)) / 60)
                        FILTER (WHERE p.fecha_primera_consulta IS NOT NULL))::numeric, 1) AS p95
                FROM paciente p
                WHERE p.fecha_registro >= :desde AND p.fecha_registro < :hasta
                GROUP BY hora_local
            )
            SELECT hora_local AT TIME ZONE :zona AS hora, total,
                SUM(total) OVER (ORDER BY hora_local) AS acumulado,
                critico, urgente, urgente2, noUrgente, noUrgente2, promedio, p95
            FROM agregados
            ORDER BY hora_local
            """;

    private final DatabaseClient db;
    private final MapeoReporteHora mapeador;

    public DaoReporteOperacionPostgres(DatabaseClient db) {
        this.db = db;
        this.mapeador = new MapeoReporteHora();
    }

    @Override
    public Flux<DtoReporteHora> consultar(RangoReporte rango, ZoneId zonaHoraria) {
        return db.sql(CONSULTA)
                .bind("zona", zonaHoraria.getId())
                .bind("desde", rango.desde().toInstant())
                .bind("hasta", rango.hasta().toInstant())
                .map((fila, metadata) -> mapeador.mapear(fila, zonaHoraria))
                .all();
    }
}
