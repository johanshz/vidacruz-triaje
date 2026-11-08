package co.vidacruz.asistente.urgencias;

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Testcontainers(disabledWithoutDocker = true)
class RedisPersistenciaTest {
    private static final Path DIRECTORIO_AOF = crearDirectorioTemporal();

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379)
            .withFileSystemBind(DIRECTORIO_AOF.toString(), "/data", BindMode.READ_WRITE)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
            .withCommand("redis-server", "--appendonly", "yes", "--dir", "/data");

    @Test
    void conservaLaVistaTrasReiniciarRedisConAof() throws Exception {
        LettuceConnectionFactory fabrica = crearFabrica();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(fabrica);
        redisTemplate.afterPropertiesSet();
        redisTemplate.opsForSet().add("urgencias:nivel:URGENTE", "paciente-1");
        Thread.sleep(Duration.ofSeconds(2).toMillis());
        redis.stop();
        redis.start();

        LettuceConnectionFactory fabricaReiniciada = crearFabrica();
        StringRedisTemplate clienteReiniciado = new StringRedisTemplate(fabricaReiniciada);
        clienteReiniciado.afterPropertiesSet();
        assertEquals(1L, clienteReiniciado.opsForSet().size("urgencias:nivel:URGENTE"));
        fabricaReiniciada.destroy();
        fabrica.destroy();
    }

    @AfterAll
    static void limpiarDirectorioAof() throws IOException {
        if (redis.isRunning()) redis.stop();
        try (var archivos = Files.walk(DIRECTORIO_AOF)) {
            for (Path archivo : archivos.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(archivo);
            }
        }
    }

    private static Path crearDirectorioTemporal() {
        try {
            return Files.createTempDirectory("asistente-redis-aof-");
        } catch (IOException excepcion) {
            throw new ExceptionInInitializerError(excepcion);
        }
    }

    private LettuceConnectionFactory crearFabrica() {
        var configuracion = new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379));
        var fabrica = new LettuceConnectionFactory(configuracion);
        fabrica.afterPropertiesSet();
        return fabrica;
    }
}
