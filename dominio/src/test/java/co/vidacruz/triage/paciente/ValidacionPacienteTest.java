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

    private NivelTriage validarYClasificar(DatosRegistroPaciente datos) {
        servicio.validar(datos);
        return servicio.clasificar(datos);
    }

    @Test void aplicaLimitesInclusivosYNormalizaQuejaAdministrativa() {
        var datos = new DatosRegistroPaciente(" Ana ", 0, 60, 110, 30, 94, new BigDecimal("34.0"), 0, 15,
                "  RENOVACION de recetario ");
        assertEquals(NivelTriage.NO_URGENTE_2, validarYClasificar(datos));
    }

    @Test void rechazaPresionDiastolicaIgualASistolica() {
        var error = assertThrows(ExcepcionValidacion.class, () -> servicio.validar(
                ServicioClasificacionPacienteTest.datos(70, 100, 100, 99, "36.0", 0, 15, "Consulta")));
        assertEquals("presionDiastolica", error.campo());
    }

    @Test void rechazaTemperaturaFueraDeRangoOMasDeUnDecimal() {
        var fuera = new DatosRegistroPaciente("Ana", 20, 70, 120, 80, 98, new BigDecimal("42.1"), 1, 15, "Dolor");
        assertEquals("temperatura", assertThrows(ExcepcionValidacion.class, () -> servicio.validar(fuera)).campo());
        var decimal = new DatosRegistroPaciente("Ana", 20, 70, 120, 80, 98, new BigDecimal("36.55"), 1, 15, "Dolor");
        assertEquals("temperatura", assertThrows(ExcepcionValidacion.class, () -> servicio.validar(decimal)).campo());
    }

    @Test void rechazaTextoRecortadoVacio() {
        var datos = new DatosRegistroPaciente("  ", 20, 70, 120, 80, 98, new BigDecimal("36.5"), 1, 15, "Dolor");
        assertEquals("nombreCompleto", assertThrows(ExcepcionValidacion.class, () -> validarYClasificar(datos)).campo());
    }

    @Test void rechazaNulosLongitudesInvalidasYExtremosFueraDeRango() {
        assertInvalid("cuerpo", null);
        var base = ServicioClasificacionPacienteTest.datos(70, 120, 80, 98, "36.5", 2, 15, "Dolor");
        assertInvalid("nombreCompleto", new DatosRegistroPaciente(null, base.edad(), base.frecuenciaCardiaca(),
                base.presionSistolica(), base.presionDiastolica(), base.spo2(), base.temperatura(), base.dolor(), base.gcs(), base.quejaPrincipal()));
        assertInvalid("quejaPrincipal", new DatosRegistroPaciente(base.nombreCompleto(), base.edad(), base.frecuenciaCardiaca(),
                base.presionSistolica(), base.presionDiastolica(), base.spo2(), base.temperatura(), base.dolor(), base.gcs(), null));
        assertInvalid("temperatura", new DatosRegistroPaciente("Ana", 20, 70, 120, 80, 98, null, 1, 15, "Dolor"));
        assertInvalid("nombreCompleto", conTexto("AB", "Dolor"));
        assertInvalid("nombreCompleto", conTexto("A".repeat(101), "Dolor"));
        assertInvalid("quejaPrincipal", conTexto("Ana", "Q".repeat(501)));
        assertInvalid("quejaPrincipal", conTexto("Ana", " "));

        assertInvalid("edad", conNumeros(-1, 70, 120, 80, 98, "36.5", 2, 15));
        assertInvalid("edad", conNumeros(121, 70, 120, 80, 98, "36.5", 2, 15));
        assertInvalid("frecuenciaCardiaca", conNumeros(40, 19, 120, 80, 98, "36.5", 2, 15));
        assertInvalid("frecuenciaCardiaca", conNumeros(40, 251, 120, 80, 98, "36.5", 2, 15));
        assertInvalid("presionSistolica", conNumeros(40, 70, 59, 40, 98, "36.5", 2, 15));
        assertInvalid("presionSistolica", conNumeros(40, 70, 261, 40, 98, "36.5", 2, 15));
        assertInvalid("presionDiastolica", conNumeros(40, 70, 120, 29, 98, "36.5", 2, 15));
        assertInvalid("presionDiastolica", conNumeros(40, 70, 200, 161, 98, "36.5", 2, 15));
        assertInvalid("spo2", conNumeros(40, 70, 120, 80, 69, "36.5", 2, 15));
        assertInvalid("spo2", conNumeros(40, 70, 120, 80, 101, "36.5", 2, 15));
        assertInvalid("temperatura", conNumeros(40, 70, 120, 80, 98, "33.9", 2, 15));
        assertInvalid("temperatura", conNumeros(40, 70, 120, 80, 98, "42.1", 2, 15));
        assertInvalid("dolor", conNumeros(40, 70, 120, 80, 98, "36.5", -1, 15));
        assertInvalid("dolor", conNumeros(40, 70, 120, 80, 98, "36.5", 11, 15));
        assertInvalid("gcs", conNumeros(40, 70, 120, 80, 98, "36.5", 2, 2));
        assertInvalid("gcs", conNumeros(40, 70, 120, 80, 98, "36.5", 2, 16));
    }

    @Test void aceptaTodosLosExtremosValidosIncluidasLongitudesDeNombre() {
        servicio.validar(conNumeros(0, 20, 60, 30, 70, "34.0", 0, 3));
        servicio.validar(conNumeros(120, 250, 260, 160, 100, "42.0", 10, 15));
        servicio.validar(new DatosRegistroPaciente("A".repeat(100), 40, 70, 120, 80, 98,
                new BigDecimal("36.5"), 2, 15, "Q".repeat(500)));
    }

    private void assertInvalid(String campo, DatosRegistroPaciente datos) {
        assertEquals(campo, assertThrows(ExcepcionValidacion.class, () -> servicio.validar(datos)).campo());
    }

    private DatosRegistroPaciente conTexto(String nombre, String queja) {
        return new DatosRegistroPaciente(nombre, 40, 70, 120, 80, 98, new BigDecimal("36.5"), 2, 15, queja);
    }

    private DatosRegistroPaciente conNumeros(int edad, int fc, int sistolica, int diastolica, int spo2,
            String temperatura, int dolor, int gcs) {
        return new DatosRegistroPaciente("Ana", edad, fc, sistolica, diastolica, spo2,
                new BigDecimal(temperatura), dolor, gcs, "Dolor");
    }
}
