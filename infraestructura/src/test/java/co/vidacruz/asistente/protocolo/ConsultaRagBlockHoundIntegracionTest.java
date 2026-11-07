package co.vidacruz.asistente.protocolo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
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
        properties = "vidacruz.ia.corpus.cargar=true")
@AutoConfigureWebTestClient
class ConsultaRagBlockHoundIntegracionTest {
    static {
        BlockHound.install();
    }

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);
    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0")
            .asCompatibleSubstituteFor("confluentinc/cp-kafka"));

    @MockitoBean
    ChatModel chatModel;
    @Autowired
    WebTestClient cliente;
    @LocalServerPort
    int puerto;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.data.redis.host", redis::getHost);
        registro.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registro.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registro.add("vidacruz.seguridad.api-key", () -> "clave-integracion-rag");
        registro.add("vidacruz.kafka.concurrencia", () -> 1);
    }

    @Test
    void consultaConProtocoloYSinCoincidenciaNoBloqueaElEventLoop() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
                new AssistantMessage("{\"respuesta\":\"Reposo por 5 min\",\"fuentes\":[{\"protocolo\":\"PROTO-001\",\"seccion\":\"Medidas iniciales\"}],\"sinCobertura\":false}")))));

        cliente.post().uri("http://localhost:" + puerto + "/asistente/consulta")
                .header("X-API-Key", "clave-integracion-rag")
                .bodyValue("{\"pregunta\":\"manejo inicial de crisis hipertensiva\"}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.fuentes[0].protocolo").isEqualTo("PROTO-001");

        cliente.post().uri("http://localhost:" + puerto + "/asistente/consulta")
                .header("X-API-Key", "clave-integracion-rag")
                .bodyValue("{\"pregunta\":\"primera línea en diabetes tipo 2\"}")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.respuesta")
                .isEqualTo("No existe un protocolo disponible para esta pregunta")
                .jsonPath("$.fuentes").isEmpty();

        verify(chatModel).call(any(Prompt.class));
    }
}
