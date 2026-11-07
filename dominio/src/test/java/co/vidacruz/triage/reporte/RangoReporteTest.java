package co.vidacruz.triage.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.triage.reporte.modelo.dto.RangoReporte;
import org.junit.jupiter.api.Test;

class RangoReporteTest {
    @Test
    void aceptaZOffsetYTreintaYUnDias() {
        var rango = RangoReporte.desdeParametros("2026-11-01T00:00:00Z", "2026-12-02T00:00:00Z");

        assertEquals("Z", rango.desde().getOffset().getId());
        assertEquals("Z", rango.hasta().getOffset().getId());
    }

    @Test
    void indicaDesdeCuandoFaltaONoEsIso8601() {
        var ausente = assertThrows(ExcepcionValidacion.class,
                () -> RangoReporte.desdeParametros(null, "2026-11-08T00:00:00Z"));
        var invalido = assertThrows(ExcepcionValidacion.class,
                () -> RangoReporte.desdeParametros("2026-11-08", "2026-11-09T00:00:00Z"));

        assertEquals("desde", ausente.campo());
        assertEquals("desde", invalido.campo());
    }

    @Test
    void indicaHastaCuandoFaltaOEsInvalidoOElRangoNoSirve() {
        var faltaHasta = assertThrows(ExcepcionValidacion.class,
                () -> RangoReporte.desdeParametros("2026-11-08T00:00:00Z", null));
        var hastaInvalido = assertThrows(ExcepcionValidacion.class,
                () -> RangoReporte.desdeParametros("2026-11-08T00:00:00Z", "fecha"));
        var ordenInvalido = assertThrows(ExcepcionValidacion.class,
                () -> RangoReporte.desdeParametros("2026-11-09T00:00:00Z", "2026-11-09T00:00:00Z"));
        var rangoLargo = assertThrows(ExcepcionValidacion.class,
                () -> RangoReporte.desdeParametros("2026-11-01T00:00:00Z", "2026-12-02T00:00:01Z"));

        assertEquals("hasta", faltaHasta.campo());
        assertEquals("hasta", hastaInvalido.campo());
        assertEquals("hasta", ordenInvalido.campo());
        assertEquals("hasta", rangoLargo.campo());
    }
}
