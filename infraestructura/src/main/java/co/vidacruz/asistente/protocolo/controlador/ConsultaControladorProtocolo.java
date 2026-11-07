package co.vidacruz.asistente.protocolo.controlador;

import co.vidacruz.asistente.protocolo.comando.ManejadorConsultarProtocolo;
import co.vidacruz.asistente.protocolo.excepcion.ExcepcionIaNoDisponible;
import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaConsulta;
import co.vidacruz.asistente.protocolo.modelo.dto.ResultadoConsulta;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/asistente")
@ConditionalOnProperty(name = "vidacruz.ia.corpus.cargar", havingValue = "true", matchIfMissing = true)
public class ConsultaControladorProtocolo {
    private static final Logger LOG = LoggerFactory.getLogger(ConsultaControladorProtocolo.class);
    private final ManejadorConsultarProtocolo manejador;
    private final MeterRegistry metricas;

    public ConsultaControladorProtocolo(ManejadorConsultarProtocolo manejador, MeterRegistry metricas) {
        this.manejador = manejador;
        this.metricas = metricas;
    }

    @PostMapping(path = "/consulta", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<RespuestaConsulta> consultar(@RequestBody Mono<JsonNode> cuerpo) {
        return cuerpo.switchIfEmpty(Mono.error(new ServerWebInputException("El cuerpo es obligatorio")))
                .flatMap(this::pregunta)
                .flatMap(manejador::manejar)
                .doOnNext(this::registrarResultado)
                .map(ResultadoConsulta::respuesta)
                .doOnError(this::registrarError);
    }

    private Mono<String> pregunta(JsonNode cuerpo) {
        JsonNode pregunta = cuerpo.path("pregunta");
        if (!cuerpo.isObject() || !pregunta.isTextual()) {
            return Mono.error(new ServerWebInputException("pregunta debe ser un texto JSON"));
        }
        return Mono.just(pregunta.textValue());
    }

    private void registrarResultado(ResultadoConsulta resultado) {
        metricas.counter("asistente.consultas", "resultado", resultado.resultado()).increment();
        if ("barrera_rechazada".equals(resultado.resultado())) {
            LOG.warn("Respuesta de protocolos rechazada por una barrera; longitudPregunta={}",
                    resultado.longitudPregunta());
        }
        LOG.info("Consulta de protocolos procesada: longitudPregunta={}, protocolos={}, resultado={}",
                resultado.longitudPregunta(), resultado.protocolosCitados(), resultado.resultado());
    }

    private void registrarError(Throwable error) {
        if (error instanceof ExcepcionIaNoDisponible) {
            metricas.counter("asistente.consultas", "resultado", "no_disponible").increment();
        }
    }
}
