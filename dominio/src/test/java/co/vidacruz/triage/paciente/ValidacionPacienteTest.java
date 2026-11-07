package co.vidacruz.triage.paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.triage.paciente.modelo.entidad.DatosRegistroPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.servicio.ServicioClasificacionPaciente;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ValidacionPacienteTest {
    private final ServicioClasificacionPaciente servicio = new ServicioClasificacionPaciente();

    @Test void aplicaLimitesInclusivosYNormalizaQuejaAdministrativa() {
        var datos = new DatosRegistroPaciente(" Ana ", 0, 60, 110, 30, 94, new BigDecimal("34.0"), 0, 15,
                "  RENOVACIÓN   DE RECETARIO ");
        assertEquals(NivelTriage.NO_URGENTE_2, servicio.clasificar(datos));
    }

    @Test void rechazaPresionDiastolicaIgualASistolica() {
        var error = assertThrows(ExcepcionValidacion.class, () -> servicio.clasificar(
                ServicioClasificacionPacienteTest.datos(70, 100, 100, 99, "36.0", 0, 15, "Consulta")));
        assertEquals("presionDiastolica", error.campo());
    }

    @Test void rechazaTemperaturaFueraDeRangoOMasDeUnDecimal() {
        var fuera = new DatosRegistroPaciente("Ana", 20, 70, 120, 80, 98, new BigDecimal("42.1"), 1, 15, "Dolor");
        assertEquals("temperatura", assertThrows(ExcepcionValidacion.class, () -> servicio.clasificar(fuera)).campo());
        var decimal = new DatosRegistroPaciente("Ana", 20, 70, 120, 80, 98, new BigDecimal("36.55"), 1, 15, "Dolor");
        assertEquals("temperatura", assertThrows(ExcepcionValidacion.class, () -> servicio.clasificar(decimal)).campo());
    }

    @Test void rechazaTextoRecortadoVacio() {
        var datos = new DatosRegistroPaciente("  ", 20, 70, 120, 80, 98, new BigDecimal("36.5"), 1, 15, "Dolor");
        assertEquals("nombreCompleto", assertThrows(ExcepcionValidacion.class, () -> servicio.clasificar(datos)).campo());
    }
}
