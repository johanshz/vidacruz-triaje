package co.vidacruz.triage.comun.excepcion;

public non-sealed class ExcepcionConflicto extends ExcepcionNegocio {

    public ExcepcionConflicto(String mensaje) {
        this(CodigoError.TRANSICION_INVALIDA, mensaje);
    }

    protected ExcepcionConflicto(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
