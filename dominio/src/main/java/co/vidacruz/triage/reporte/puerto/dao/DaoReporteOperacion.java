package co.vidacruz.triage.reporte.puerto.dao;

import co.vidacruz.triage.reporte.modelo.dto.DtoReporteHora;
import co.vidacruz.triage.reporte.modelo.dto.RangoReporte;
import java.time.ZoneId;
import reactor.core.publisher.Flux;

public interface DaoReporteOperacion {
    Flux<DtoReporteHora> consultar(RangoReporte rango, ZoneId zonaHoraria);
}
