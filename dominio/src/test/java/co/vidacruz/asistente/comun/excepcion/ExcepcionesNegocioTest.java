package co.vidacruz.asistente.comun.excepcion;

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import co.vidacruz.asistente.urgencias.excepcion.ExcepcionEventoInvalido;
import co.vidacruz.asistente.urgencias.excepcion.ExcepcionUrgenciasNoDisponible;
import org.junit.jupiter.api.Test;

class ExcepcionesNegocioTest {
    @Test
    void exponenCodigoCampoMensajeYCausaDelContrato() {
        var validacion = new ExcepcionValidacion("nivel", "El nivel no es válido");
        assertEquals("VALIDACION", validacion.codigo());
        assertEquals("nivel", validacion.campo());
        assertEquals("El nivel no es válido", validacion.getMessage());

        var noEncontrado = new ExcepcionRecursoNoEncontrado("No existe");
        assertEquals("NO_ENCONTRADO", noEncontrado.codigo());
        var eventoInvalido = new ExcepcionEventoInvalido("Falta tipo");
        assertEquals("EVENTO_INVALIDO", eventoInvalido.codigo());

        var causa = new IllegalStateException("Redis no responde");
        var noDisponible = new ExcepcionUrgenciasNoDisponible(causa);
        assertEquals("URGENCIAS_NO_DISPONIBLE", noDisponible.codigo());
        assertSame(causa, noDisponible.getCause());
    }
}
