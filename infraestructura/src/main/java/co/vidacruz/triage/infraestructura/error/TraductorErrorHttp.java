package co.vidacruz.triage.infraestructura.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

@Component
public class TraductorErrorHttp {
    public ErrorHttp traducir(HttpStatusCode status) {
        if (status.is5xxServerError()) return new ErrorHttp(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error interno");
        if (status.value() == 404) return new ErrorHttp(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Ruta o recurso inexistente");
        return new ErrorHttp(status, "SOLICITUD_INVALIDA", "La solicitud no se pudo procesar");
    }
    public record ErrorHttp(HttpStatusCode status, String codigo, String detalle) { }
}
