package co.vidacruz.triage.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import co.vidacruz.triage.reporte.consulta.ManejadorConsultarReporteOperacion;
import co.vidacruz.triage.reporte.modelo.dto.DtoReporteHora;
import co.vidacruz.triage.reporte.modelo.dto.RangoReporte;
import co.vidacruz.triage.reporte.puerto.dao.DaoReporteOperacion;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class ManejadorConsultarReporteOperacionTest {
    private final DaoReporteOperacion dao = mock(DaoReporteOperacion.class);
    private final ZoneId zona = ZoneId.of("America/Bogota");
    private final ManejadorConsultarReporteOperacion manejador = new ManejadorConsultarReporteOperacion(dao, zona);

    @Test
    void construyeElSobreDelReporteConZonaYHorasDelDao() {
        var desde = "2026-11-08T00:00:00-05:00";
        var hasta = "2026-11-09T00:00:00-05:00";
        var hora = new DtoReporteHora(java.time.OffsetDateTime.parse("2026-11-08T08:00:00-05:00"),
                3, 3, Map.of("CRITICO", 3, "URGENTE", 0, "URGENTE_2", 0, "NO_URGENTE", 0,
                        "NO_URGENTE_2", 0), null, null);
        when(dao.consultar(RangoReporte.desdeParametros(desde, hasta), zona)).thenReturn(Flux.just(hora));

        StepVerifier.create(manejador.ejecutar(desde, hasta))
                .assertNext(reporte -> {
                    assertEquals(java.time.OffsetDateTime.parse(desde), reporte.desde());
                    assertEquals(java.time.OffsetDateTime.parse(hasta), reporte.hasta());
                    assertEquals("America/Bogota", reporte.zonaHoraria());
                    assertEquals(3, reporte.horas().getFirst().acumuladoRegistrados());
                }).verifyComplete();
        verify(dao).consultar(RangoReporte.desdeParametros(desde, hasta), zona);
    }

    @Test
    void rangoVacioDevuelveHorasVacias() {
        when(dao.consultar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(zona)))
                .thenReturn(Flux.empty());

        StepVerifier.create(manejador.ejecutar("2026-11-08T00:00:00Z", "2026-11-09T00:00:00Z"))
                .assertNext(reporte -> assertEquals(0, reporte.horas().size())).verifyComplete();
    }
}
