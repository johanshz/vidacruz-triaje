package co.vidacruz.asistente.comun.excepcion;

public final class ExcepcionValidacion extends ExcepcionNegocio {
    private final String campo;

    public ExcepcionValidacion(String campo, String mensaje) {
        super("VALIDACION", mensaje);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
