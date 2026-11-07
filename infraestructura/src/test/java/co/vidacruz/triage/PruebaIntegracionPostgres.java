package co.vidacruz.triage;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.blockhound.BlockHound;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class PruebaIntegracionPostgres {
    private static final AtomicBoolean BLOCKHOUND_INSTALADO = new AtomicBoolean();
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16").withReuse(true);

    static { POSTGRES.start(); }

    @BeforeAll static void instalarBlockHoundParaIntegracion() {
        if (BLOCKHOUND_INSTALADO.compareAndSet(false, true)) BlockHound.install();
    }

    @DynamicPropertySource static void registrarPropiedades(DynamicPropertyRegistry registry) {
        registry.add("DB_HOST", POSTGRES::getHost);
        registry.add("DB_PORT", () -> POSTGRES.getMappedPort(5432));
        registry.add("DB_NAME", POSTGRES::getDatabaseName);
        registry.add("DB_USER", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("TRIAGE_API_KEY", () -> "clave-prueba");
        registry.add("SERVER_PORT", () -> "0");
        registry.add("vidacruz.outbox.intervalo", () -> "PT1H");
        registry.add("vidacruz.outbox.retraso-inicial", () -> "PT1H");
    }
}
