package co.vidacruz.triage.comun.excepcion;

public class ExcepcionRecursoNoEncontrado extends ExcepcionNegocio {

    public ExcepcionRecursoNoEncontrado(String mensaje) {
        super("NO_ENCONTRADO", mensaje);
    }
}
