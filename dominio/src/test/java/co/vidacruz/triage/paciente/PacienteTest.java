package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PacienteTest {
    private static final Instant REGISTRO = Instant.parse("2026-11-06T10:00:00Z");

    @Test void registraLaPrimeraEntradaAConsultaYLaConservaAlDarAlta() {
        var paciente = paciente();
        Instant consulta = REGISTRO.plusSeconds(420);
        var enConsulta = paciente.cambiarEstado(EstadoPaciente.EN_CONSULTA, consulta);
        var dadoDeAlta = enConsulta.cambiarEstado(EstadoPaciente.ALTA, consulta.plusSeconds(300));

        assertEquals(consulta, enConsulta.fechaPrimeraConsulta());
        assertEquals(consulta, dadoDeAlta.fechaPrimeraConsulta());
        assertEquals(EstadoPaciente.ALTA, dadoDeAlta.estado());
    }

    @Test void elAltaDirectaNoRegistraFechaDePrimeraConsulta() {
        var dadoDeAlta = paciente().cambiarEstado(EstadoPaciente.ALTA, REGISTRO.plusSeconds(30));

        assertNull(dadoDeAlta.fechaPrimeraConsulta());
    }

    private Paciente paciente() {
        var datos = new DatosRegistroPaciente("Ana", 30, 80, 120, 80, 98,
                new BigDecimal("36.5"), 1, 15, "Dolor");
        return new Paciente(UUID.randomUUID(), datos, NivelTriage.NO_URGENTE, EstadoPaciente.REGISTRADO,
                false, REGISTRO, REGISTRO, null);
    }
}
