package co.vidacruz.asistente.urgencias.excepcion;

import co.vidacruz.asistente.comun.excepcion.ExcepcionNegocio;

public final class ExcepcionEventoInvalido extends ExcepcionNegocio {
    public ExcepcionEventoInvalido(String mensaje) {
        super("EVENTO_INVALIDO", mensaje);
    }
}
