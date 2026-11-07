package co.vidacruz.triage.evento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.vidacruz.triage.evento.modelo.entidad.DatosEventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.modelo.entidad.TipoEventoPaciente;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventoPacienteTest {
    @Test void representaTipoYVersionContractuales() {
        var paciente = UUID.randomUUID();
        var evento = new EventoPaciente(UUID.randomUUID(), TipoEventoPaciente.PACIENTE_REGISTRADO.valor(),
                EventoPaciente.VERSION_ACTUAL, Instant.parse("2026-11-06T10:00:00Z"),
                new DatosEventoPaciente(paciente, "URGENTE", null, "REGISTRADO", false));

        assertEquals("paciente_registrado", evento.tipo());
        assertEquals(1, evento.version());
        assertEquals(paciente, evento.datos().idPaciente());
    }

    @Test void rechazaVersionNoSoportada() {
        assertThrows(IllegalArgumentException.class, () -> new EventoPaciente(UUID.randomUUID(),
                TipoEventoPaciente.PACIENTE_REGISTRADO.valor(), EventoPaciente.VERSION_ACTUAL + 1,
                Instant.now(), new DatosEventoPaciente(UUID.randomUUID(), "URGENTE", null, "REGISTRADO", false)));
    }
}
