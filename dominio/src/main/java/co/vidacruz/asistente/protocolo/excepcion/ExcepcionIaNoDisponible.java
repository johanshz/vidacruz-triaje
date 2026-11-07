package co.vidacruz.asistente.protocolo.excepcion;

import co.vidacruz.asistente.comun.excepcion.ExcepcionNegocio;

public final class ExcepcionIaNoDisponible extends ExcepcionNegocio {
    public ExcepcionIaNoDisponible(Throwable causa) {
        super("IA_NO_DISPONIBLE", "El asistente clínico no está disponible en este momento. Consulte los protocolos impresos.");
        initCause(causa);
    }
}
