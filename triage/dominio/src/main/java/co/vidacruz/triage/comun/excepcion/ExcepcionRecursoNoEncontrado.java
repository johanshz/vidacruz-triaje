package co.vidacruz.triage.comun.excepcion;

public non-sealed class ExcepcionRecursoNoEncontrado extends ExcepcionNegocio {

    public ExcepcionRecursoNoEncontrado(String mensaje) {
        super(CodigoError.NO_ENCONTRADO, mensaje);
    }
}
