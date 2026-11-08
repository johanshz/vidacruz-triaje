package co.vidacruz.asistente.comun.excepcion;

public final class ExcepcionRecursoNoEncontrado extends ExcepcionNegocio {
    public ExcepcionRecursoNoEncontrado(String mensaje) {
        super("NO_ENCONTRADO", mensaje);
    }
}
