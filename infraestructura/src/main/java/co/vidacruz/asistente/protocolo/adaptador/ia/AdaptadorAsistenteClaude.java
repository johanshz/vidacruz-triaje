package co.vidacruz.asistente.protocolo.adaptador.ia;

import co.vidacruz.asistente.protocolo.excepcion.ExcepcionIaNoDisponible;
import co.vidacruz.asistente.protocolo.modelo.dto.RespuestaGenerada;
import co.vidacruz.asistente.protocolo.modelo.entidad.FragmentoProtocolo;
import co.vidacruz.asistente.protocolo.puerto.dao.AsistenteProtocolos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class AdaptadorAsistenteClaude implements AsistenteProtocolos {
    private static final Logger LOG = LoggerFactory.getLogger(AdaptadorAsistenteClaude.class);
    private static final String METRICA_LATENCIA = "asistente.llm.latencia";
    private static final String ETIQUETA_RESULTADO = "resultado";
    private static final String RESULTADO_OK = "ok";
    private static final String RESULTADO_ERROR = "error";
    private static final String RESULTADO_CIRCUITO_ABIERTO = "circuito_abierto";
    private static final String INSTRUCCIONES = "Eres un asistente clínico limitado a los fragmentos oficiales recibidos. "
            + "No agregues conocimiento externo. Devuelve únicamente JSON con campos respuesta (texto), "
            + "fuentes (lista de objetos protocolo y seccion) y sinCobertura (boolean). "
            + "Cita solo fragmentos entregados. Si los fragmentos tratan el tema de la pregunta aunque no den "
            + "el detalle exacto pedido, responde con lo que sí dicen y aclara qué no especifican. Solo si los "
            + "fragmentos no tratan el tema de la pregunta, usa respuesta vacía, fuentes vacías y sinCobertura true.";

    private final ChatClient chatClient;
    private final ObjectMapper mapper;
    private final DependenciasAsistenteClaude dependencias;

    public AdaptadorAsistenteClaude(ChatClient chatClient, ObjectMapper mapper,
            DependenciasAsistenteClaude dependencias) {
        this.chatClient = chatClient;
        this.mapper = mapper;
        this.dependencias = dependencias;
    }

    @Override
    public Mono<RespuestaGenerada> responder(String pregunta, List<FragmentoProtocolo> fragmentos) {
        return Mono.defer(() -> {
            Timer.Sample medicion = Timer.start(dependencias.meterRegistry());
            AtomicReference<String> resultado = new AtomicReference<>(RESULTADO_OK);
            return Mono.fromCallable(() -> generarJson(pregunta, fragmentos))
                    .subscribeOn(Schedulers.boundedElastic())
                    .transformDeferred(TimeLimiterOperator.of(dependencias.timeLimiter()))
                    .transformDeferred(CircuitBreakerOperator.of(dependencias.circuitBreaker()))
                    .doOnError(error -> resultado.set(error instanceof CallNotPermittedException
                            ? RESULTADO_CIRCUITO_ABIERTO : RESULTADO_ERROR))
                    .doFinally(senal -> medicion.stop(dependencias.meterRegistry().timer(
                            METRICA_LATENCIA, ETIQUETA_RESULTADO, resultado.get())))
                    .onErrorMap(error -> new ExcepcionIaNoDisponible(error));
        });
    }

    private RespuestaGenerada generarJson(String pregunta, List<FragmentoProtocolo> fragmentos) {
        String contenido = solicitarContenido(pregunta, fragmentos);
        LOG.debug("Respuesta cruda del modelo: {}", contenido);
        return deserializar(contenido);
    }

    private String solicitarContenido(String pregunta, List<FragmentoProtocolo> fragmentos) {
        return chatClient.prompt()
                .system(INSTRUCCIONES)
                .user(construirPrompt(pregunta, fragmentos))
                .call()
                .content();
    }

    private RespuestaGenerada deserializar(String contenido) {
        try {
            RespuestaGenerada respuesta = mapper.readValue(extraerObjetoJson(contenido), RespuestaGenerada.class);
            return respuesta == null ? RespuestaGenerada.invalida() : respuesta;
        } catch (JsonProcessingException | NullPointerException | IllegalArgumentException error) {
            return RespuestaGenerada.invalida();
        }
    }

    /**
     * El modelo suele envolver el JSON en un bloque markdown (```json ... ```); sin quitarlo, Jackson
     * falla y toda respuesta válida termina rechazada por la barrera (verificado en vivo 2026-11-07).
     */
    private String extraerObjetoJson(String contenido) {
        if (contenido == null) {
            return null;
        }
        int inicio = contenido.indexOf('{');
        int fin = contenido.lastIndexOf('}');
        return inicio >= 0 && fin > inicio ? contenido.substring(inicio, fin + 1) : contenido;
    }

    private String construirPrompt(String pregunta, List<FragmentoProtocolo> fragmentos) {
        String contexto = fragmentos.stream()
                .map(fragmento -> "[" + fragmento.codigo() + " | " + fragmento.seccion() + "] " + fragmento.texto())
                .collect(Collectors.joining("\n"));
        return "Pregunta clínica:\n" + pregunta + "\n\nFragmentos oficiales:\n" + contexto
                + "\n\nResponde conforme al formato JSON indicado.";
    }
}
