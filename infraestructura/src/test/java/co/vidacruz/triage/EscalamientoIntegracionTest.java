package co.vidacruz.triage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import co.vidacruz.triage.evento.adaptador.repositorio.AdaptadorR2dbcOutbox;
import co.vidacruz.triage.vigilancia.comando.ComandoEscalarVencidos;
import co.vidacruz.triage.vigilancia.comando.ManejadorEscalarVencidos;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EscalamientoIntegracionTest extends SoporteIntegracionTriage {
    @Autowired private ManejadorEscalarVencidos manejador;
    @Autowired private MeterRegistry registroMetricas;
    @MockitoSpyBean private AdaptadorR2dbcOutbox outbox;

    @Test
    void dosEjecucionesConcurrentesEscribenUnSoloEscalamientoYUnaMetrica() {
        UUID id = UUID.randomUUID();
        StepVerifier.create(insertarPacienteVencido(id)).expectNext(1L).verifyComplete();
        double antes = contadorEscalamientos();
        AtomicLong escalablesPendientes = new AtomicLong();
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM paciente "
                + "WHERE estado = 'REGISTRADO' AND escalado = false "
                + "AND fecha_registro + (sla_minutos * INTERVAL '1 minute') <= now()")
                .map((fila, metadata) -> fila.get("total", Long.class)).one())
                .assertNext(escalablesPendientes::set).verifyComplete();

        StepVerifier.create(Mono.when(manejador.ejecutar(new ComandoEscalarVencidos()),
                manejador.ejecutar(new ComandoEscalarVencidos()))).verifyComplete();

        StepVerifier.create(contar("SELECT COUNT(*) AS total FROM auditoria WHERE paciente_id = :id "
                + "AND tipo_evento = 'paciente_escalado'", id)).expectNext(1L).verifyComplete();
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM outbox WHERE llave = :id "
                + "AND tipo = 'paciente_escalado'").bind("id", id.toString())
                .map((fila, metadata) -> fila.get("total", Long.class)).one()).expectNext(1L).verifyComplete();
        assertThat(contadorEscalamientos() - antes).isEqualTo(escalablesPendientes.get());
    }

    @Test
    void falloAlGuardarOutboxRevierteFlagYAuditoria() {
        UUID id = UUID.randomUUID();
        StepVerifier.create(insertarPacienteVencido(id)).expectNext(1L).verifyComplete();
        doReturn(Mono.error(new IllegalStateException("fallo forzado")))
                .when(outbox).agregar(any(), anyString());

        StepVerifier.create(manejador.ejecutar(new ComandoEscalarVencidos()))
                .expectError(IllegalStateException.class).verify();

        StepVerifier.create(databaseClient.sql("SELECT escalado FROM paciente WHERE id = :id")
                .bind("id", id).map((fila, metadata) -> fila.get("escalado", Boolean.class)).one())
                .expectNext(false).verifyComplete();
        StepVerifier.create(contar("SELECT COUNT(*) AS total FROM auditoria WHERE paciente_id = :id "
                + "AND tipo_evento = 'paciente_escalado'", id)).expectNext(0L).verifyComplete();
    }

    private Mono<Long> insertarPacienteVencido(UUID id) {
        Instant fecha = Instant.parse("2020-01-01T00:00:00Z");
        return databaseClient.sql("""
                INSERT INTO paciente (id, nombre_completo, edad, frecuencia_cardiaca, presion_sistolica,
                    presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal, nivel,
                    sla_minutos, estado, escalado, fecha_registro, fecha_estado)
                VALUES (:id, 'Paciente escalable', 30, 80, 120, 80, 98, 36.5, 0, 15, 'Dolor',
                    'CRITICO', 1, 'REGISTRADO', false, :fecha, :fecha)
                """).bind("id", id).bind("fecha", fecha).fetch().rowsUpdated();
    }

    private Mono<Long> contar(String sql, UUID id) {
        return databaseClient.sql(sql).bind("id", id)
                .map((fila, metadata) -> fila.get("total", Long.class)).one();
    }

    private double contadorEscalamientos() {
        return registroMetricas.get("triaje.escalamientos").counter().count();
    }
}
