package co.vidacruz.triage.comun.excepcion;

public class ExcepcionConflicto extends ExcepcionNegocio {

    public ExcepcionConflicto(String mensaje) {
        super("TRANSICION_INVALIDA", mensaje);
    }
}
