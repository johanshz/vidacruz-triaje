package co.vidacruz.triage.infraestructura.error;

import co.vidacruz.triage.comun.excepcion.CodigoError;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TraductorErrorHttp {
    public ErrorHttp traducir(HttpStatusCode status) {
        if (status.is5xxServerError()) return ErrorHttp.interno();
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) return ErrorHttp.noEncontrado();
        return ErrorHttp.solicitudInvalida(status);
    }
    public record ErrorHttp(HttpStatusCode status, CodigoError codigo, String detalle, Optional<String> campo) {
        public ErrorHttp {
            campo = campo == null ? Optional.empty() : campo;
        }

        public static ErrorHttp sinCampo(HttpStatusCode status, CodigoError codigo, String detalle) {
            return new ErrorHttp(status, codigo, detalle, Optional.empty());
        }

        public static ErrorHttp conCampo(HttpStatusCode status, CodigoError codigo, String detalle, String campo) {
            return new ErrorHttp(status, codigo, detalle, Optional.of(campo));
        }

        public static ErrorHttp interno() {
            return sinCampo(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO, MensajesError.INTERNO);
        }

        public static ErrorHttp noEncontrado() {
            return sinCampo(HttpStatus.NOT_FOUND, CodigoError.NO_ENCONTRADO, MensajesError.NO_ENCONTRADO);
        }

        public static ErrorHttp solicitudInvalida(HttpStatusCode status) {
            return sinCampo(status, CodigoError.SOLICITUD_INVALIDA, "La solicitud no se pudo procesar");
        }

        public static ErrorHttp validacion(String detalle, Optional<String> campo) {
            return campo
                .map(valor -> conCampo(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, detalle, valor))
                .orElseGet(() -> sinCampo(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION, detalle));
        }
    }
}
