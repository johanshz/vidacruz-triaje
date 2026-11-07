package co.vidacruz.asistente.protocolo;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.blockhound.BlockHound;

/**
 * Prueba de CA-22. Se omite explícitamente si el modelo ONNX fijado por la RFC no está en el classpath;
 * esa omisión se registra como PE y nunca se reemplaza por embeddings simulados.
 */
@EnabledIf("co.vidacruz.asistente.protocolo.ModeloOnnxCondicion#estaDisponible")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"vidacruz.ia.corpus.cargar=true", "spring.ai.model.chat=anthropic",
                "spring.ai.model.embedding=transformers",
                "spring.ai.embedding.transformer.onnx.model-uri=classpath:/model/model.onnx",
                "spring.ai.embedding.transformer.tokenizer.uri=classpath:/model/tokenizer.json",
                // El application.yaml de pruebas oculta el principal: se repiten aquí sus valores de IA
                "vidacruz.ia.modelo=claude-sonnet-5-5", "vidacruz.ia.umbral-similitud=0.55",
                "vidacruz.ia.top-k=3", "vidacruz.ia.resiliencia.timeout=10s",
                "vidacruz.ia.resiliencia.timeout-embeddings=10s", "vidacruz.ia.resiliencia.ventana=10",
                "vidacruz.ia.resiliencia.minimo-llamadas=5", "vidacruz.ia.resiliencia.porcentaje-fallos=50",
                "vidacruz.ia.resiliencia.espera-abierto=30s",
                "vidacruz.ia.resiliencia.llamadas-semiabierto=2"})
@AutoConfigureWebTestClient
class ConsultaRagBlockHoundIntegracionTest {
    static {
        BlockHound.install();
    }

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);
    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"));

    @MockitoBean
    ChatModel chatModel;
    @Autowired
    WebTestClient cliente;
    @Autowired
    CircuitBreakerRegistry circuitos;
    @LocalServerPort
    int puerto;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.data.redis.host", redis::getHost);
        registro.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registro.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registro.add("vidacruz.seguridad.api-key", () -> "clave-integracion-rag");
        registro.add("spring.ai.anthropic.api-key", () -> "");
        registro.add("vidacruz.kafka.concurrencia", () -> 1);
    }

    @Test
    void consultaConProtocoloYSinCoincidenciaNoBloqueaElEventLoop() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
                new AssistantMessage("{\"respuesta\":\"Reposo por 5 min\",\"fuentes\":[{\"protocolo\":\"PROTO-001\",\"seccion\":\"Medidas iniciales\"}],\"sinCobertura\":false}")))));

        cliente.post().uri("http://localhost:" + puerto + "/asistente/consulta")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "clave-integracion-rag")
                .bodyValue("{\"pregunta\":\"manejo inicial de crisis hipertensiva\"}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.fuentes[0].protocolo").isEqualTo("PROTO-001");

        cliente.post().uri("http://localhost:" + puerto + "/asistente/consulta")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "clave-integracion-rag")
                .bodyValue("{\"pregunta\":\"primera línea en diabetes tipo 2\"}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.respuesta")
                .isEqualTo("No existe un protocolo disponible para esta pregunta")
                .jsonPath("$.fuentes").isEmpty();

        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void validaErroresHttpYClaveVaciaPermiteArranque() {
        cliente.post().uri("http://localhost:" + puerto + "/asistente/consulta")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"pregunta\":\"pregunta válida\"}")
                .exchange().expectStatus().isUnauthorized();

        cliente.post().uri("http://localhost:" + puerto + "/asistente/consulta")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "clave-integracion-rag")
                .bodyValue("{\"pregunta\":")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.codigo").isEqualTo("VALIDACION");

        circuitos.circuitBreaker("asistenteProtocolos").transitionToOpenState();
        long inicio = System.nanoTime();
        cliente.post().uri("http://localhost:" + puerto + "/asistente/consulta")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", "clave-integracion-rag")
                .bodyValue("{\"pregunta\":\"manejo inicial de crisis hipertensiva\"}")
                .exchange().expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.codigo").isEqualTo("IA_NO_DISPONIBLE");
        assertTrue(Duration.ofNanos(System.nanoTime() - inicio)
                .compareTo(Duration.ofSeconds(2)) < 0);
        verifyNoInteractions(chatModel);
    }
}
