package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import co.vidacruz.triage.paciente.modelo.entidad.ConfiguracionSla;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import org.junit.jupiter.api.Test;

class ConfiguracionSlaTest {
    @Test
    void conservaLosValoresPredeterminadosPorNivel() {
        var configuracion = new ConfiguracionSla(5, 10, 30, 60, 120);

        assertEquals(5, configuracion.minutos(NivelTriage.CRITICO));
        assertEquals(10, configuracion.minutos(NivelTriage.URGENTE));
        assertEquals(30, configuracion.minutos(NivelTriage.URGENTE_2));
        assertEquals(60, configuracion.minutos(NivelTriage.NO_URGENTE));
        assertEquals(120, configuracion.minutos(NivelTriage.NO_URGENTE_2));
    }
}
