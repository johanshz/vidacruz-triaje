package co.vidacruz.asistente.urgencias.excepcion;

import co.vidacruz.asistente.comun.excepcion.ExcepcionNegocio;

public final class ExcepcionUrgenciasNoDisponible extends ExcepcionNegocio {
    public ExcepcionUrgenciasNoDisponible(Throwable causa) {
        super("URGENCIAS_NO_DISPONIBLE", "No fue posible consultar el estado de urgencias");
        initCause(causa);
    }
}
