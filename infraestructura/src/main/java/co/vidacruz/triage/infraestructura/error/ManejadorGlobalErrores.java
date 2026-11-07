package co.vidacruz.triage.infraestructura.error;

import co.vidacruz.triage.comun.excepcion.ExcepcionConflicto;
import co.vidacruz.triage.comun.excepcion.ExcepcionRecursoNoEncontrado;
import co.vidacruz.triage.comun.excepcion.ExcepcionValidacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.resource.NoResourceFoundException;

@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);
    private final FabricaProblemDetail fabrica;

    public ManejadorGlobalErrores(FabricaProblemDetail fabrica) {
        this.fabrica = fabrica;
    }

    @ExceptionHandler(ExcepcionValidacion.class)
    public ResponseEntity<ProblemDetail> validacion(ExcepcionValidacion exception, ServerWebExchange exchange) {
        return respuesta(exchange, HttpStatus.BAD_REQUEST, exception.codigo(), exception.getMessage(), exception.campo());
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ProblemDetail> entradaInvalida(ServerWebInputException exception, ServerWebExchange exchange) {
        String campo = campoQueFallo(exception);
        return respuesta(exchange, HttpStatus.BAD_REQUEST, "VALIDACION", "La solicitud contiene datos inválidos", campo);
    }

    @ExceptionHandler(ExcepcionRecursoNoEncontrado.class)
    public ResponseEntity<ProblemDetail> noEncontrado(ExcepcionRecursoNoEncontrado exception, ServerWebExchange exchange) {
        return respuesta(exchange, HttpStatus.NOT_FOUND, exception.codigo(), exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> errorHttp(ResponseStatusException exception, ServerWebExchange exchange) {
        return respuestaHttp(exchange, exception.getStatusCode(), exception);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> recursoHttpNoEncontrado(NoResourceFoundException exception,
                                                                  ServerWebExchange exchange) {
        return respuesta(exchange, HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Ruta o recurso inexistente");
    }

    @ExceptionHandler(ExcepcionConflicto.class)
    public ResponseEntity<ProblemDetail> conflicto(ExcepcionConflicto exception, ServerWebExchange exchange) {
        return respuesta(exchange, HttpStatus.CONFLICT, exception.codigo(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> errorInterno(Exception exception, ServerWebExchange exchange) {
        LOG.error("Error no controlado traceId={}", fabrica.traceId(exchange), exception);
        return respuesta(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
            "Ocurrió un error interno");
    }

    private ResponseEntity<ProblemDetail> respuesta(ServerWebExchange exchange, HttpStatus status,
                                                     String codigo, String detalle, String campo) {
        return ResponseEntity.status(status).body(fabrica.crear(exchange, status, codigo, detalle, campo));
    }

    private ResponseEntity<ProblemDetail> respuesta(ServerWebExchange exchange, HttpStatus status, String codigo, String detalle) {
        return ResponseEntity.status(status).body(fabrica.crear(exchange, status, codigo, detalle));
    }

    private String campoQueFallo(ServerWebInputException exception) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof JsonMappingException mismatch) {
                var path = mismatch.getPath();
                return path.isEmpty() ? null : path.get(path.size() - 1).getFieldName();
            }
            causa = causa.getCause();
        }
        return null;
    }

    private ResponseEntity<ProblemDetail> respuestaHttp(ServerWebExchange exchange, HttpStatusCode status,
                                                         ResponseStatusException exception) {
        var traduccion = new TraductorErrorHttp().traducir(status);
        if (status.is5xxServerError()) LOG.error("Error HTTP no controlado traceId={}", fabrica.traceId(exchange), exception);
        return ResponseEntity.status(traduccion.status()).body(
            fabrica.crear(exchange, traduccion.status(), traduccion.codigo(), traduccion.detalle()));
    }}
