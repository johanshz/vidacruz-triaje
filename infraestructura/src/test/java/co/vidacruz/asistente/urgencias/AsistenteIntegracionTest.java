package co.vidacruz.asistente.urgencias;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.blockhound.BlockHound;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(OutputCaptureExtension.class)
class AsistenteIntegracionTest {
    static {
        BlockHound.install();
    }
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
            .withCommand("redis-server", "--appendonly", "yes");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

    @LocalServerPort private int puerto;
    private static WebTestClient cliente;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.data.redis.host", redis::getHost);
        registro.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registro.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registro.add("vidacruz.seguridad.api-key", () -> "clave-integracion");
        registro.add("vidacruz.kafka.concurrencia", () -> 1);
    }

    @Autowired
    void asignarCliente(WebTestClient webTestClient) {
        cliente = webTestClient;
    }

    @Test
    @Order(0)
    void arrancaSinTopicDeEventosYCreaLaDltConTresParticiones() throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", kafka.getBootstrapServers()))) {
            var dlt = admin.describeTopics(List.of("triaje.eventos.DLT")).allTopicNames()
                    .get(10, TimeUnit.SECONDS).get("triaje.eventos.DLT");
            org.junit.jupiter.api.Assertions.assertEquals(3, dlt.partitions().size());
            admin.createTopics(List.of(new org.apache.kafka.clients.admin.NewTopic("triaje.eventos", 3, (short) 1)))
                    .all().get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(1)
    void consumeEventosRepetidosYAltaYExponeConteos(CapturedOutput salida) throws Exception {
        String id = UUID.randomUUID().toString();
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        try (KafkaProducer<String, String> productor = new KafkaProducer<>(propiedadesProductor())) {
            ProducerRecord<String, String> registro = new ProducerRecord<>("triaje.eventos", id, evento(id, "paciente_registrado", "URGENTE"));
            registro.headers().add("traceparent", ("00-" + traceId + "-00f067aa0ba902b7-01").getBytes(StandardCharsets.UTF_8));
            productor.send(registro).get(10, TimeUnit.SECONDS);
            productor.send(new ProducerRecord<>("triaje.eventos", id, evento(id, "paciente_registrado", "URGENTE"))).get(10, TimeUnit.SECONDS);
        }
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> cliente.get()
                .uri("http://localhost:" + puerto + "/asistente/estado-urgencias")
                .header("X-API-Key", "clave-integracion")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.porNivel.URGENTE").isEqualTo(1));
        org.junit.jupiter.api.Assertions.assertTrue(salida.getOut().contains(traceId),
                "El traceId del evento debe aparecer en los logs del consumidor");

        try (KafkaProducer<String, String> productor = new KafkaProducer<>(propiedadesProductor())) {
            productor.send(new ProducerRecord<>("triaje.eventos", id, evento(id, "paciente_alta", null))).get(10, TimeUnit.SECONDS);
        }
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> cliente.get()
                .uri("http://localhost:" + puerto + "/asistente/estado-urgencias")
                .header("X-API-Key", "clave-integracion")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.totalActivos").isEqualTo(0));
    }

    @Test
    @Order(2)
    void protegeConsultaYDejaProbesAbiertasConTraceId(CapturedOutput salida) {
        var sinClave = cliente.get().uri("http://localhost:" + puerto + "/asistente/estado-urgencias")
                .exchange().expectStatus().isUnauthorized()
                .expectHeader().exists("X-Trace-Id")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO").returnResult();
        String traceId401 = sinClave.getResponseHeaders().getFirst("X-Trace-Id");
        org.junit.jupiter.api.Assertions.assertTrue(new String(sinClave.getResponseBody(), StandardCharsets.UTF_8)
                .contains("\"traceId\":\"" + traceId401 + "\""));
        cliente.post().uri("http://localhost:" + puerto + "/asistente/consulta")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"pregunta\":\"consulta de protocolo\"}")
                .exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTORIZADO");
        cliente.get().uri("http://localhost:" + puerto + "/actuator/health/liveness")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP");
        cliente.get().uri("http://localhost:" + puerto + "/actuator/health/readiness")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP");
        cliente.get().uri("http://localhost:" + puerto + "/asistente/estado-urgencias")
                .header("X-API-Key", "clave-incorrecta").exchange().expectStatus().isUnauthorized();
        cliente.get().uri("http://localhost:" + puerto + "/actuator/info")
                .exchange().expectStatus().isUnauthorized();
        cliente.get().uri("http://localhost:" + puerto + "/actuator/info")
                .header("X-API-Key", "clave-integracion").exchange().expectStatus().isOk();
        cliente.get().uri("http://localhost:" + puerto + "/actuator/prometheus")
                .exchange().expectStatus().isUnauthorized();
        cliente.get().uri("http://localhost:" + puerto + "/actuator/prometheus")
                .header("X-API-Key", "clave-integracion").exchange().expectStatus().isOk();
        cliente.post().uri("http://localhost:" + puerto + "/asistente/estado-urgencias")
                .header("X-API-Key", "clave-integracion").exchange().expectStatus().isEqualTo(405)
                .expectHeader().exists("X-Trace-Id")
                .expectBody().jsonPath("$.codigo").isEqualTo("SOLICITUD_INVALIDA");
        var noEncontrado = cliente.get().uri("http://localhost:" + puerto + "/ruta-inexistente")
                .header("X-API-Key", "clave-integracion").exchange().expectStatus().isNotFound()
                .expectHeader().exists("X-Trace-Id")
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO").returnResult();
        String traceId404 = noEncontrado.getResponseHeaders().getFirst("X-Trace-Id");
        org.junit.jupiter.api.Assertions.assertTrue(new String(noEncontrado.getResponseBody(), StandardCharsets.UTF_8)
                .contains("\"traceId\":\"" + traceId404 + "\""));
        org.junit.jupiter.api.Assertions.assertTrue(salida.getOut().contains(traceId401));
        org.junit.jupiter.api.Assertions.assertTrue(salida.getOut().contains(traceId404));
    }

    @Test
    @Order(3)
    void devuelveConteosPorNivelTrasDosRegistrosOtroNivelYUnAlta() throws Exception {
        redis.execInContainer("redis-cli", "FLUSHDB");
        String criticoUno = UUID.randomUUID().toString();
        String criticoDos = UUID.randomUUID().toString();
        String noUrgente = UUID.randomUUID().toString();
        try (KafkaProducer<String, String> productor = new KafkaProducer<>(propiedadesProductor())) {
            productor.send(new ProducerRecord<>("triaje.eventos", criticoUno,
                    evento(criticoUno, "paciente_registrado", "CRITICO"))).get(10, TimeUnit.SECONDS);
            productor.send(new ProducerRecord<>("triaje.eventos", criticoDos,
                    evento(criticoDos, "paciente_registrado", "CRITICO"))).get(10, TimeUnit.SECONDS);
            productor.send(new ProducerRecord<>("triaje.eventos", noUrgente,
                    evento(noUrgente, "paciente_registrado", "NO_URGENTE_2"))).get(10, TimeUnit.SECONDS);
            productor.send(new ProducerRecord<>("triaje.eventos", criticoUno,
                    evento(criticoUno, "paciente_alta", null))).get(10, TimeUnit.SECONDS);
        }
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> cliente.get()
                .uri("http://localhost:" + puerto + "/asistente/estado-urgencias")
                .header("X-API-Key", "clave-integracion").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.porNivel.CRITICO").isEqualTo(1)
                .jsonPath("$.porNivel.NO_URGENTE_2").isEqualTo(1)
                .jsonPath("$.totalActivos").isEqualTo(2));
    }

    @Test
    @Order(4)
    void enviaJsonMalFormadoALaDltSinReintentoYContinuaConElSiguienteOffset() throws Exception {
        redis.execInContainer("redis-cli", "FLUSHDB");
        Properties configuracion = new Properties();
        configuracion.put("bootstrap.servers", kafka.getBootstrapServers());
        configuracion.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID());
        configuracion.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configuracion.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configuracion.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> consumidor = new KafkaConsumer<>(configuracion)) {
            consumidor.subscribe(List.of("triaje.eventos.DLT"));
            consumidor.poll(Duration.ofMillis(500));
            try (KafkaProducer<String, String> productor = new KafkaProducer<>(propiedadesProductor())) {
                productor.send(new ProducerRecord<>("triaje.eventos", "p-invalido", "esto-no-es-json"))
                        .get(10, TimeUnit.SECONDS);
            }
            AtomicReference<ConsumerRecord<String, String>> recibido = new AtomicReference<>();
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var registros = consumidor.poll(Duration.ofMillis(250));
                if (!registros.isEmpty()) recibido.set(registros.iterator().next());
                org.junit.jupiter.api.Assertions.assertNotNull(recibido.get());
            });
            org.junit.jupiter.api.Assertions.assertEquals("esto-no-es-json", recibido.get().value());
        }
        String id = UUID.randomUUID().toString();
        try (KafkaProducer<String, String> productor = new KafkaProducer<>(propiedadesProductor())) {
            productor.send(new ProducerRecord<>("triaje.eventos", id,
                    evento(id, "paciente_registrado", "CRITICO"))).get(10, TimeUnit.SECONDS);
        }
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> cliente.get()
                .uri("http://localhost:" + puerto + "/asistente/estado-urgencias")
                .header("X-API-Key", "clave-integracion")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.porNivel.CRITICO").isEqualTo(1));
    }

    @Test
    @Order(5)
    void reintentaFalloTransitorioDeRedisYTieneDltFinal() throws Exception {
        String id = UUID.randomUUID().toString();
        Properties configuracion = new Properties();
        configuracion.put("bootstrap.servers", kafka.getBootstrapServers());
        configuracion.put(ConsumerConfig.GROUP_ID_CONFIG, "retry-test-" + UUID.randomUUID());
        configuracion.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configuracion.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configuracion.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> consumidor = new KafkaConsumer<>(configuracion)) {
            consumidor.subscribe(List.of("triaje.eventos.DLT"));
            consumidor.poll(Duration.ofMillis(500));
            redis.stop();
            cliente.get().uri("http://localhost:" + puerto + "/actuator/health/readiness")
                    .exchange().expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP");
            cliente.get().uri("http://localhost:" + puerto + "/asistente/estado-urgencias")
                    .header("X-API-Key", "clave-integracion").exchange().expectStatus().isEqualTo(503)
                    .expectBody().jsonPath("$.codigo").isEqualTo("URGENCIAS_NO_DISPONIBLE");
            try (KafkaProducer<String, String> productor = new KafkaProducer<>(propiedadesProductor())) {
                productor.send(new ProducerRecord<>("triaje.eventos", id,
                        evento(id, "paciente_registrado", "URGENTE"))).get(10, TimeUnit.SECONDS);
            }
            AtomicReference<ConsumerRecord<String, String>> recibido = new AtomicReference<>();
            await().atMost(Duration.ofSeconds(35)).untilAsserted(() -> {
                var registros = consumidor.poll(Duration.ofMillis(250));
                registros.forEach(registro -> {
                    if (registro.value() != null && registro.value().contains(id)) recibido.set(registro);
                });
                org.junit.jupiter.api.Assertions.assertNotNull(recibido.get());
            });
            org.junit.jupiter.api.Assertions.assertTrue(recibido.get().value().contains(id));
        } finally {
            redis.start();
        }
    }

    private static Properties propiedadesProductor() {
        Properties propiedades = new Properties();
        propiedades.put("bootstrap.servers", kafka.getBootstrapServers());
        propiedades.put("key.serializer", StringSerializer.class.getName());
        propiedades.put("value.serializer", StringSerializer.class.getName());
        return propiedades;
    }

    private static String evento(String id, String tipo, String nivel) {
        String nivelJson = nivel == null ? "null" : "\"" + nivel + "\"";
        return "{\"idEvento\":\"" + UUID.randomUUID() + "\",\"tipo\":\"" + tipo
                + "\",\"version\":1,\"ocurridoEn\":\"2026-11-08T10:00:00Z\",\"datos\":{\"idPaciente\":\""
                + id + "\",\"nivel\":" + nivelJson + ",\"estadoAnterior\":null,\"estadoNuevo\":\"REGISTRADO\",\"escalado\":false}}";
    }
}
