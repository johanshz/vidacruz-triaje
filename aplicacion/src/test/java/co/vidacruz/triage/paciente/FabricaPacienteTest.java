package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import co.vidacruz.triage.evento.modelo.entidad.TipoEventoPaciente;
import co.vidacruz.triage.paciente.comando.FabricaPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FabricaPacienteTest {
    @Test void creaPacienteYEventoDeRegistroConValoresNormalizados() {
        var datos = new DatosRegistroPaciente(" Ana ", 40, 70, 120, 80, 98,
                new BigDecimal("36.5"), 2, 15, " Dolor ");
        var instante = Instant.parse("2026-11-06T10:00:00Z");
        var paciente = new FabricaPaciente().registrado(datos, NivelTriage.NO_URGENTE, 60, UUID.randomUUID(), instante);
        var evento = new FabricaPaciente().eventoRegistrado(paciente, UUID.randomUUID(), instante);

        assertEquals("Ana", paciente.datos().nombreCompleto());
        assertEquals("Dolor", paciente.datos().quejaPrincipal());
        assertEquals(EstadoPaciente.REGISTRADO, paciente.estado());
        assertEquals(TipoEventoPaciente.PACIENTE_REGISTRADO.valor(), evento.tipo());
        assertEquals(paciente.id(), evento.datos().idPaciente());
        assertEquals("NO_URGENTE", evento.datos().nivel());
        assertNull(evento.datos().estadoAnterior());
        assertEquals("REGISTRADO", evento.datos().estadoNuevo());
    }
}
