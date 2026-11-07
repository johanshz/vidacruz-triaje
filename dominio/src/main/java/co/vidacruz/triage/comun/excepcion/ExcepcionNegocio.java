package co.vidacruz.triage.comun.excepcion;

public abstract class ExcepcionNegocio extends RuntimeException {

    private final String codigo;

    protected ExcepcionNegocio(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
