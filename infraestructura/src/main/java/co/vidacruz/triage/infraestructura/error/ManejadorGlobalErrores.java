package co.vidacruz.triage.infraestructura.error;

import co.vidacruz.triage.comun.excepcion.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.util.Optional;

@RestControllerAdvice
public class ManejadorGlobalErrores {
    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);
    private final FabricaProblemDetail fabrica;
    private final TraductorErrorHttp traductor;

    public ManejadorGlobalErrores(FabricaProblemDetail fabrica, TraductorErrorHttp traductor) {
        this.fabrica = fabrica;
        this.traductor = traductor;
    }

    @ExceptionHandler(ExcepcionValidacion.class)
    public ResponseEntity<ProblemDetail> validacion(ExcepcionValidacion exception, ServerWebExchange exchange) {
        return respuesta(exchange, TraductorErrorHttp.ErrorHttp.validacion(exception.getMessage(), Optional.of(exception.campo())));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ProblemDetail> entradaInvalida(ServerWebInputException exception, ServerWebExchange exchange) {
        return respuesta(exchange, TraductorErrorHttp.ErrorHttp.validacion(MensajesError.DETALLE_VALIDACION, campoQueFallo(exception)));
    }

    @ExceptionHandler(ExcepcionRecursoNoEncontrado.class)
    public ResponseEntity<ProblemDetail> noEncontrado(ExcepcionRecursoNoEncontrado exception, ServerWebExchange exchange) {
        return respuesta(exchange, TraductorErrorHttp.ErrorHttp.sinCampo(HttpStatus.NOT_FOUND, exception.codigo(), exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> errorHttp(ResponseStatusException exception, ServerWebExchange exchange) {
        return respuestaHttp(exchange, exception.getStatusCode(), exception);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> recursoHttpNoEncontrado(NoResourceFoundException exception, ServerWebExchange exchange) {
        return respuesta(exchange, TraductorErrorHttp.ErrorHttp.noEncontrado());
    }

    @ExceptionHandler(ExcepcionConflicto.class)
    public ResponseEntity<ProblemDetail> conflicto(ExcepcionConflicto exception, ServerWebExchange exchange) {
        return respuesta(exchange, TraductorErrorHttp.ErrorHttp.sinCampo(HttpStatus.CONFLICT, exception.codigo(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> errorInterno(Exception exception, ServerWebExchange exchange) {
        LOG.error("Error no controlado traceId={}", exchange.getAttribute(FiltroTraceId.ATRIBUTO_TRACE_ID), exception);
        return respuesta(exchange, TraductorErrorHttp.ErrorHttp.interno());
    }

    private ResponseEntity<ProblemDetail> respuesta(ServerWebExchange exchange, TraductorErrorHttp.ErrorHttp error) {
        return ResponseEntity.status(error.status()).body(fabrica.crear(exchange, error));
    }

    private Optional<String> campoQueFallo(ServerWebInputException exception) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof JsonMappingException mismatch) {
                var ruta = mismatch.getPath();
                return ruta.isEmpty() ? Optional.empty() : Optional.ofNullable(ruta.get(ruta.size() - 1).getFieldName());
            }
            causa = causa.getCause();
        }
        return Optional.empty();
    }

    private ResponseEntity<ProblemDetail> respuestaHttp(ServerWebExchange exchange, org.springframework.http.HttpStatusCode status,
                                                         ResponseStatusException exception) {
        var traduccion = traductor.traducir(status);
        if (status.is5xxServerError()) LOG.error("Error HTTP no controlado traceId={}", exchange.getAttribute(FiltroTraceId.ATRIBUTO_TRACE_ID), exception);
        return respuesta(exchange, traduccion);
    }
}
