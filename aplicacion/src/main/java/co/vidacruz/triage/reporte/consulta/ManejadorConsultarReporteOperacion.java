package co.vidacruz.triage.reporte.consulta;

import co.vidacruz.triage.reporte.modelo.dto.DtoReporteOperacion;
import co.vidacruz.triage.reporte.modelo.dto.RangoReporte;
import co.vidacruz.triage.reporte.puerto.dao.DaoReporteOperacion;
import java.time.ZoneId;
import reactor.core.publisher.Mono;

public class ManejadorConsultarReporteOperacion {
    private final DaoReporteOperacion dao;
    private final ZoneId zonaHoraria;

    public ManejadorConsultarReporteOperacion(DaoReporteOperacion dao, ZoneId zonaHoraria) {
        this.dao = dao;
        this.zonaHoraria = zonaHoraria;
    }

    public Mono<DtoReporteOperacion> ejecutar(String desde, String hasta) {
        RangoReporte rango = RangoReporte.desdeParametros(desde, hasta);
        return dao.consultar(rango, zonaHoraria)
                .collectList()
                .map(horas -> new DtoReporteOperacion(rango.desde(), rango.hasta(), zonaHoraria.getId(), horas));
    }
}
