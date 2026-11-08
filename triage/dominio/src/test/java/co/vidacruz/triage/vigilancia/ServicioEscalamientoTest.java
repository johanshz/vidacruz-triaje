package co.vidacruz.triage.vigilancia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.vigilancia.servicio.ServicioEscalamiento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicioEscalamientoTest {
    private static final Instant REGISTRO = Instant.parse("2026-11-06T10:00:00Z");
    private final ServicioEscalamiento servicio = new ServicioEscalamiento();

    @Test
    void venceEnElInstanteExactoDelSla() {
        assertFalse(servicio.estaVencido(paciente(EstadoPaciente.REGISTRADO, false), REGISTRO.plusSeconds(59)));
        assertTrue(servicio.estaVencido(paciente(EstadoPaciente.REGISTRADO, false), REGISTRO.plusSeconds(60)));
    }

    @Test
    void pacienteYaEscaladoNoVuelveASeleccionarse() {
        assertFalse(servicio.estaVencido(paciente(EstadoPaciente.REGISTRADO, true), REGISTRO.plusSeconds(60)));
    }

    @Test
    void pacienteEnConsultaOAltaNoSeEscala() {
        assertFalse(servicio.estaVencido(paciente(EstadoPaciente.EN_CONSULTA, false), REGISTRO.plusSeconds(60)));
        assertFalse(servicio.estaVencido(paciente(EstadoPaciente.ALTA, false), REGISTRO.plusSeconds(60)));
    }

    private Paciente paciente(EstadoPaciente estado, boolean escalado) {
        var datos = new DatosRegistroPaciente("Paciente de prueba", 40, 80, 120, 80, 98,
                new BigDecimal("36.5"), 1, 15, "Dolor");
        return new Paciente(UUID.randomUUID(), datos, NivelTriage.CRITICO, 1, estado, escalado,
                REGISTRO, REGISTRO, null);
    }
}
