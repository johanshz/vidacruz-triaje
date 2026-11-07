package co.vidacruz.asistente.configuracion;

import co.vidacruz.asistente.comun.excepcion.ExcepcionNegocio;
import co.vidacruz.asistente.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.vidacruz.asistente.comun.excepcion.ExcepcionValidacion;
import co.vidacruz.asistente.urgencias.excepcion.ExcepcionUrgenciasNoDisponible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class ManejadorGlobalErrores {
    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    @ExceptionHandler(ExcepcionValidacion.class)
    ResponseEntity<ProblemDetail> validacion(ExcepcionValidacion excepcion, ServerWebExchange intercambio) {
        ProblemDetail problema = crear(HttpStatus.BAD_REQUEST, excepcion.getMessage(), excepcion.codigo(), intercambio, true);
        problema.setProperty("campo", excepcion.campo());
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(ExcepcionRecursoNoEncontrado.class)
    ResponseEntity<ProblemDetail> noEncontrado(ExcepcionRecursoNoEncontrado excepcion, ServerWebExchange intercambio) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(crear(HttpStatus.NOT_FOUND, excepcion.getMessage(), excepcion.codigo(), intercambio, true));
    }

    @ExceptionHandler(ExcepcionUrgenciasNoDisponible.class)
    ResponseEntity<ProblemDetail> urgenciasNoDisponible(ExcepcionUrgenciasNoDisponible excepcion, ServerWebExchange intercambio) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(crear(HttpStatus.SERVICE_UNAVAILABLE, excepcion.getMessage(), excepcion.codigo(), intercambio, true));
    }

    @ExceptionHandler(ServerWebInputException.class)
    ResponseEntity<ProblemDetail> entradaInvalida(ServerWebInputException excepcion, ServerWebExchange intercambio) {
        return ResponseEntity.badRequest().body(crear(HttpStatus.BAD_REQUEST, "La solicitud no es válida", "VALIDACION", intercambio, true));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> estadoHttp(ResponseStatusException excepcion, ServerWebExchange intercambio) {
        HttpStatus estado = HttpStatus.valueOf(excepcion.getStatusCode().value());
        String codigo = estado == HttpStatus.NOT_FOUND ? "NO_ENCONTRADO" : "SOLICITUD_INVALIDA";
        String detalle = estado == HttpStatus.NOT_FOUND ? "La ruta solicitada no existe" : "La solicitud no es válida";
        return ResponseEntity.status(estado).body(crear(estado, detalle, codigo, intercambio, true));
    }

    @ExceptionHandler(ExcepcionNegocio.class)
    ResponseEntity<ProblemDetail> negocio(ExcepcionNegocio excepcion, ServerWebExchange intercambio) {
        return ResponseEntity.badRequest().body(crear(HttpStatus.BAD_REQUEST, excepcion.getMessage(), excepcion.codigo(), intercambio, true));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> interno(Exception excepcion, ServerWebExchange intercambio) {
        LOG.error("Error no controlado al atender la solicitud", excepcion);
        return ResponseEntity.internalServerError()
                .body(crear(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno", "ERROR_INTERNO", intercambio, false));
    }

    private ProblemDetail crear(HttpStatus estado, String detalle, String codigo,
            ServerWebExchange intercambio, boolean incluirTraceId) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(estado.getReasonPhrase());
        problema.setProperty("codigo", codigo);
        problema.setProperty("traceId", intercambio.getAttributeOrDefault("traceId", "desconocido"));
        return problema;
    }
}
