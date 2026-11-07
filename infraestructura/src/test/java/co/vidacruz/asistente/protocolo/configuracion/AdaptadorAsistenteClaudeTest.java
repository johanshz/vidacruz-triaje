package co.vidacruz.asistente.protocolo.configuracion;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.vidacruz.asistente.configuracion.PropiedadesIa;
import co.vidacruz.asistente.protocolo.adaptador.ia.AdaptadorAsistenteClaude;
import co.vidacruz.asistente.protocolo.adaptador.ia.DependenciasAsistenteClaude;
import co.vidacruz.asistente.protocolo.excepcion.ExcepcionIaNoDisponible;
import co.vidacruz.asistente.protocolo.modelo.entidad.FragmentoProtocolo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import reactor.test.StepVerifier;

class AdaptadorAsistenteClaudeTest {
    private static final String PREGUNTA = "pregunta clínica confidencial";
    private static final String RESPUESTA_JSON = "{\"respuesta\":\"respuesta clínica privada\","
            + "\"fuentes\":[],\"sinCobertura\":true}";
    private static final List<FragmentoProtocolo> FRAGMENTOS = List.of(
            new FragmentoProtocolo("PROTO-001", "Crisis", "Medidas iniciales", "reposo 5 minutos"));

    @Test
    void propagaTimeoutConexionYRespuestasHttpComoNoDisponibleYAbreCircuitoSinReintentar() {
        var registro = new SimpleMeterRegistry();
        var modelo = mock(ChatModel.class);
        var configuracion = new ConfiguracionAsistenteProtocolos();
        var propiedades = propiedades(Duration.ofMillis(80), 10);
        CircuitBreakerRegistry circuitos = configuracion.circuitBreakerRegistry(propiedades);
        TimeLimiterRegistry limitadores = configuracion.timeLimiterRegistry(propiedades);
        CircuitBreaker circuito = circuitos.circuitBreaker("asistenteProtocolos");
        AdaptadorAsistenteClaude adaptador = new AdaptadorAsistenteClaude(
                ChatClient.builder(modelo).build(), new ObjectMapper(),
                new DependenciasAsistenteClaude(circuito, limitadores.timeLimiter("asistenteProtocolos"), registro));
        AtomicInteger llamadas = new AtomicInteger();
        when(modelo.call(any(Prompt.class))).thenAnswer(invocacion -> switch (llamadas.incrementAndGet()) {
            case 1 -> {
                Thread.sleep(300);
                yield respuestaChat();
            }
            case 2 -> throw new ResourceAccessException("fallo de conexión");
            case 3 -> throw HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR,
                    "error", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
            case 4 -> throw HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS,
                    "rate limit", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
            case 5 -> throw HttpClientErrorException.create(HttpStatus.UNAUTHORIZED,
                    "unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
            case 6 -> throw HttpClientErrorException.create(HttpStatus.FORBIDDEN,
                    "forbidden", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
            default -> respuestaChat();
        });

        for (int numero = 0; numero < 6; numero++) {
            StepVerifier.create(adaptador.responder(PREGUNTA, FRAGMENTOS))
                    .expectError(ExcepcionIaNoDisponible.class)
                    .verify(Duration.ofSeconds(2));
        }

        assertEquals(6, circuito.getMetrics().getNumberOfFailedCalls(),
                "Los timeouts, errores de conexión y respuestas 5xx/429/401/403 cuentan como fallos");
        await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> assertEquals(6,
                registro.get("asistente.llm.latencia").tag("resultado", "error").timer().count()));

        circuito.transitionToOpenState();
        long inicio = System.nanoTime();
        StepVerifier.create(adaptador.responder(PREGUNTA, FRAGMENTOS))
                .expectError(ExcepcionIaNoDisponible.class)
                .verify(Duration.ofSeconds(2));
        long duracionMs = Duration.ofNanos(System.nanoTime() - inicio).toMillis();

        assertTrue(duracionMs < 2_000, "El circuito abierto debe responder antes de 2 segundos");
        verify(modelo, times(6)).call(any(Prompt.class));
        await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> assertEquals(1,
                registro.get("asistente.llm.latencia").tag("resultado", "circuito_abierto").timer().count()));
    }

    @Test
    void registraResultadoExitosoEnElTimerDelProveedor() {
        var registro = new SimpleMeterRegistry();
        var modelo = mock(ChatModel.class);
        var adaptador = crearAdaptador(modelo, registro, Duration.ofSeconds(1), 5);
        when(modelo.call(any(Prompt.class))).thenReturn(respuestaChat());

        StepVerifier.create(adaptador.responder(PREGUNTA, FRAGMENTOS))
                .expectNextCount(1).verifyComplete();

        await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> assertEquals(1,
                registro.get("asistente.llm.latencia").tag("resultado", "ok").timer().count()));
    }

    @Test
    void jsonInvalidoEsRespuestaInvalidaSinAbrirElCircuito() {
        var modelo = mock(ChatModel.class);
        var adaptador = crearAdaptador(modelo, new SimpleMeterRegistry(), Duration.ofSeconds(1), 5);
        when(modelo.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
                new Generation(new AssistantMessage("esto no es json")))));

        StepVerifier.create(adaptador.responder(PREGUNTA, FRAGMENTOS))
                .assertNext(respuesta -> assertTrue(respuesta.sinCobertura()))
                .verifyComplete();
    }

    @Test
    void requestAlModeloNoIncluyeTemperatureNiThinkingDesactivado() throws Exception {
        var modelo = mock(ChatModel.class);
        var adaptador = crearAdaptador(modelo, new SimpleMeterRegistry(), Duration.ofSeconds(1), 5);
        when(modelo.call(any(Prompt.class))).thenReturn(respuestaChat());

        StepVerifier.create(adaptador.responder(PREGUNTA, FRAGMENTOS))
                .expectNextCount(1).verifyComplete();

        var captor = forClass(Prompt.class);
        verify(modelo).call(captor.capture());
        ChatOptions opciones = captor.getValue().getOptions();
        assertTrue(opciones == null || opciones.getTemperature() == null,
                "No se debe enviar temperature");
        if (opciones != null) {
            assertFalse(thinkingDesactivado(opciones), "No se debe enviar thinking desactivado");
        }
    }

    private AdaptadorAsistenteClaude crearAdaptador(ChatModel modelo, SimpleMeterRegistry registro,
            Duration timeout, int minimoLlamadas) {
        var configuracion = new ConfiguracionAsistenteProtocolos();
        var propiedades = propiedades(timeout, minimoLlamadas);
        return new AdaptadorAsistenteClaude(ChatClient.builder(modelo).build(), new ObjectMapper(),
                new DependenciasAsistenteClaude(
                        configuracion.circuitBreakerRegistry(propiedades).circuitBreaker("asistenteProtocolos"),
                        configuracion.timeLimiterRegistry(propiedades).timeLimiter("asistenteProtocolos"), registro));
    }

    private PropiedadesIa propiedades(Duration timeout, int minimoLlamadas) {
        return new PropiedadesIa("claude-sonnet-5-5", 0.75, 3,
                new PropiedadesIa.Resiliencia(timeout, timeout, 10, minimoLlamadas, 50.0f,
                        Duration.ofSeconds(30), 2));
    }

    private ChatResponse respuestaChat() {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(RESPUESTA_JSON))));
    }

    private boolean thinkingDesactivado(ChatOptions opciones) throws Exception {
        try {
            Method obtenerThinking = opciones.getClass().getMethod("getThinking");
            Object thinking = obtenerThinking.invoke(opciones);
            if (thinking == null) return false;
            try {
                Object tipo = thinking.getClass().getMethod("getType").invoke(thinking);
                return "disabled".equalsIgnoreCase(String.valueOf(tipo));
            } catch (NoSuchMethodException ignorada) {
                Object habilitado = thinking.getClass().getMethod("isEnabled").invoke(thinking);
                return Boolean.FALSE.equals(habilitado);
            }
        } catch (NoSuchMethodException ignorada) {
            return false;
        }
    }
}
