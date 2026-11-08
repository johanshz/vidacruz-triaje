package co.vidacruz.triage.comun.excepcion;

public abstract sealed class ExcepcionNegocio extends RuntimeException
    permits ExcepcionValidacion, ExcepcionRecursoNoEncontrado, ExcepcionConflicto {

    private final CodigoError codigo;

    protected ExcepcionNegocio(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public CodigoError codigo() {
        return codigo;
    }
}
