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
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class AdaptadorAsistenteClaude implements AsistenteProtocolos {
    private static final String INSTRUCCIONES = "Eres un asistente clínico limitado a los fragmentos oficiales recibidos. "
            + "No agregues conocimiento externo. Devuelve únicamente JSON con campos respuesta (texto), "
            + "fuentes (lista de objetos protocolo y seccion) y sinCobertura (boolean). "
            + "Cita solo fragmentos entregados. Si no cubren la pregunta, usa respuesta vacía, fuentes vacías y sinCobertura true.";

    private final ChatClient chatClient;
    private final ObjectMapper mapper;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final MeterRegistry meterRegistry;

    public AdaptadorAsistenteClaude(ChatClient chatClient, ObjectMapper mapper,
            CircuitBreaker circuitBreaker, TimeLimiter timeLimiter,
            MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.mapper = mapper;
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<RespuestaGenerada> responder(String pregunta, List<FragmentoProtocolo> fragmentos) {
        return Mono.defer(() -> {
            Timer.Sample medicion = Timer.start(meterRegistry);
            AtomicReference<String> resultado = new AtomicReference<>("ok");
            return Mono.fromCallable(() -> generarJson(pregunta, fragmentos))
                    .subscribeOn(Schedulers.boundedElastic())
                    .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .doOnError(error -> resultado.set(error instanceof CallNotPermittedException
                            ? "circuito_abierto" : "error"))
                    .doFinally(senal -> medicion.stop(meterRegistry.timer(
                            "asistente.llm.latencia", "resultado", resultado.get())))
                    .onErrorMap(error -> new ExcepcionIaNoDisponible(error));
        });
    }

    private RespuestaGenerada generarJson(String pregunta, List<FragmentoProtocolo> fragmentos) {
        String prompt = construirPrompt(pregunta, fragmentos);
        String contenido = chatClient.prompt()
                .system(INSTRUCCIONES)
                .user(prompt)
                .call()
                .content();
        try {
            RespuestaGenerada respuesta = mapper.readValue(contenido, RespuestaGenerada.class);
            return respuesta == null ? RespuestaGenerada.invalida() : respuesta;
        } catch (JsonProcessingException error) {
            return RespuestaGenerada.invalida();
        }
    }

    private String construirPrompt(String pregunta, List<FragmentoProtocolo> fragmentos) {
        String contexto = fragmentos.stream()
                .map(fragmento -> "[" + fragmento.codigo() + " | " + fragmento.seccion() + "] " + fragmento.texto())
                .collect(Collectors.joining("\n"));
        return "Pregunta clínica:\n" + pregunta + "\n\nFragmentos oficiales:\n" + contexto
                + "\n\nResponde conforme al formato JSON indicado.";
    }
}
