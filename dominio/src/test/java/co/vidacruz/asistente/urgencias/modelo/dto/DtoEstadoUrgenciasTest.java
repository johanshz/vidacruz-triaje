package co.vidacruz.asistente.urgencias.modelo.dto;

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import co.vidacruz.asistente.urgencias.modelo.entidad.NivelUrgencias;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DtoEstadoUrgenciasTest {
    @Test
    void siempreExponeCincoNivelesEnOrdenConCerosYSuma() {
        Map<NivelUrgencias, Long> conteos = new LinkedHashMap<>();
        conteos.put(NivelUrgencias.URGENTE, 2L);
        conteos.put(NivelUrgencias.NO_URGENTE_2, 1L);
        Instant momento = Instant.parse("2026-11-08T10:00:00Z");

        DtoEstadoUrgencias estado = new DtoEstadoUrgencias(999, conteos, momento);

        assertEquals(List.of(NivelUrgencias.values()), List.copyOf(estado.porNivel().keySet()));
        assertEquals(0L, estado.porNivel().get(NivelUrgencias.CRITICO));
        assertEquals(2L, estado.porNivel().get(NivelUrgencias.URGENTE));
        assertEquals(3L, estado.totalActivos());
        assertEquals(momento, estado.consultadoEn());
        assertNotNull(estado.consultadoEn());
    }

    @Test
    void estadoVacioTieneTodosLosNivelesEnCero() {
        DtoEstadoUrgencias estado = new DtoEstadoUrgencias(0, Map.of(), Instant.now());
        assertEquals(5, estado.porNivel().size());
        assertEquals(0L, estado.totalActivos());
        assertEquals(List.of(0L, 0L, 0L, 0L, 0L), List.copyOf(estado.porNivel().values()));
    }
}
