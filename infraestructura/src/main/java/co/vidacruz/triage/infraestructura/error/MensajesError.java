package co.vidacruz.triage.infraestructura.error;

public final class MensajesError {
    public static final String INTERNO = "Ocurrió un error interno";
    public static final String NO_ENCONTRADO = "Ruta o recurso inexistente";
    public static final String API_KEY_INVALIDA = "API key ausente o inválida";
    public static final String DETALLE_VALIDACION = "La solicitud contiene datos inválidos";

    private MensajesError() { }
}
