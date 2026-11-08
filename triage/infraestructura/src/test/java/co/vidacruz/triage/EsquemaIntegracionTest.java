package co.vidacruz.triage;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class EsquemaIntegracionTest extends SoporteIntegracionTriage {
    @Test
    void flywayCreaLasTablasColumnasIndicesYDisparadoresDelContrato() {
        var columnas = databaseClient.sql("SELECT table_name || '.' || column_name AS nombre FROM information_schema.columns WHERE table_schema = 'public' AND table_name IN ('paciente', 'auditoria', 'outbox')")
            .map((fila, metadata) -> fila.get("nombre", String.class)).all().collectList();
        StepVerifier.create(columnas).assertNext(nombres -> assertTrue(nombres.containsAll(List.of(
            "paciente.id", "paciente.nombre_completo", "paciente.edad", "paciente.frecuencia_cardiaca",
            "paciente.presion_sistolica", "paciente.presion_diastolica", "paciente.spo2", "paciente.temperatura",
            "paciente.dolor", "paciente.gcs", "paciente.queja_principal", "paciente.nivel", "paciente.sla_minutos",
            "paciente.estado", "paciente.escalado", "paciente.fecha_registro", "paciente.fecha_estado",
            "paciente.fecha_primera_consulta", "auditoria.id", "auditoria.paciente_id", "auditoria.tipo_evento",
            "auditoria.estado_anterior", "auditoria.estado_nuevo", "auditoria.ocurrido_en", "outbox.id",
            "outbox.secuencia", "outbox.tipo", "outbox.llave", "outbox.carga", "outbox.creado_en", "outbox.enviado_en")))).verifyComplete();
        StepVerifier.create(databaseClient.sql("SELECT indexname FROM pg_indexes WHERE schemaname = 'public'")
                .map((fila, metadata) -> fila.get("indexname", String.class)).all().collectList())
            .assertNext(nombres -> assertTrue(nombres.containsAll(List.of("idx_paciente_estado_escalado_fecha",
                "idx_paciente_fecha_registro", "idx_auditoria_paciente_id", "idx_outbox_pendiente_secuencia"))))
            .verifyComplete();
        StepVerifier.create(databaseClient.sql("SELECT tgname FROM pg_trigger WHERE NOT tgisinternal")
                .map((fila, metadata) -> fila.get("tgname", String.class)).all().collectList())
            .assertNext(nombres -> assertTrue(nombres.containsAll(List.of("auditoria_inmutable_filas", "auditoria_inmutable_truncate"))))
            .verifyComplete();
    }

    @Test
    void auditoriaPermiteInsertar() {
        UUID pacienteId = UUID.randomUUID();
        StepVerifier.create(insertarPaciente(pacienteId).then(insertarAuditoria(pacienteId)))
            .expectNext(1L).verifyComplete();
    }

    @Test
    void auditoriaRechazaUpdateDeleteYTruncate() {
        UUID pacienteId = UUID.randomUUID();
        UUID auditoriaId = UUID.randomUUID();
        StepVerifier.create(insertarPaciente(pacienteId).then(insertarAuditoria(pacienteId, auditoriaId)))
            .expectNext(1L).verifyComplete();
        verificarAuditoriaInmutable(databaseClient.sql("UPDATE auditoria SET tipo_evento = 'paciente_alta' WHERE id = :id")
            .bind("id", auditoriaId).fetch().rowsUpdated());
        verificarAuditoriaInmutable(databaseClient.sql("DELETE FROM auditoria WHERE id = :id")
            .bind("id", auditoriaId).fetch().rowsUpdated());
        verificarAuditoriaInmutable(databaseClient.sql("TRUNCATE auditoria").fetch().rowsUpdated());
    }

    @Test
    void checkRechazaCatalogosDesconocidos() {
        UUID pacienteId = UUID.randomUUID();
        StepVerifier.create(insertarPaciente(pacienteId, "NIVEL_INVALIDO")).expectError().verify();
        StepVerifier.create(insertarPaciente(UUID.randomUUID(), "URGENTE", "ESTADO_INVALIDO")).expectError().verify();
        StepVerifier.create(databaseClient.sql("INSERT INTO auditoria (id, paciente_id, tipo_evento, ocurrido_en) VALUES (:id, :paciente, 'tipo_invalido', now())")
            .bind("id", UUID.randomUUID()).bind("paciente", pacienteId).fetch().rowsUpdated()).expectError().verify();
        StepVerifier.create(insertarOutbox(UUID.randomUUID(), "tipo_invalido")).expectError().verify();
    }

    @Test
    void outboxAsignaSecuenciaCreciente() {
        UUID pacienteId = UUID.randomUUID();
        StepVerifier.create(insertarOutbox(pacienteId, "paciente_registrado")
                .flatMap(primera -> insertarOutbox(pacienteId, "paciente_alta").map(segunda -> primera < segunda)))
            .assertNext(secuenciaCreciente -> assertTrue(secuenciaCreciente)).verifyComplete();
    }
}
