package co.vidacruz.triage;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

abstract class SoporteIntegracionTriage extends PruebaIntegracionPostgres {
    @Autowired protected DatabaseClient databaseClient;
    @Autowired protected WebTestClient webTestClient;

    protected Mono<Long> insertarPaciente(UUID id) {
        return insertarPaciente(id, "URGENTE", "REGISTRADO");
    }

    protected Mono<Long> insertarPaciente(UUID id, String nivel) {
        return insertarPaciente(id, nivel, "REGISTRADO");
    }

    protected Mono<Long> insertarPaciente(UUID id, String nivel, String estado) {
        return databaseClient.sql("INSERT INTO paciente (id, nombre_completo, edad, frecuencia_cardiaca, "
                + "presion_sistolica, presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal, "
                + "nivel, sla_minutos, estado, fecha_registro, fecha_estado) VALUES (:id, 'Paciente Prueba', 30, "
                + "80, 120, 80, 98, 36.5, 0, 15, 'Dolor de cabeza', :nivel, 30, :estado, :fecha, :fecha)")
            .bind("id", id).bind("nivel", nivel).bind("estado", estado)
            .bind("fecha", OffsetDateTime.now(ZoneOffset.UTC)).fetch().rowsUpdated();
    }

    protected Mono<Long> insertarAuditoria(UUID pacienteId) {
        return insertarAuditoria(pacienteId, UUID.randomUUID());
    }

    protected Mono<Long> insertarAuditoria(UUID pacienteId, UUID auditoriaId) {
        return databaseClient.sql("INSERT INTO auditoria (id, paciente_id, tipo_evento, estado_nuevo, ocurrido_en) "
                + "VALUES (:id, :paciente, 'paciente_registrado', 'REGISTRADO', :fecha)")
            .bind("id", auditoriaId).bind("paciente", pacienteId)
            .bind("fecha", OffsetDateTime.now(ZoneOffset.UTC)).fetch().rowsUpdated();
    }

    protected Mono<Long> insertarOutbox(UUID pacienteId, String tipo) {
        return databaseClient.sql("INSERT INTO outbox (id, tipo, llave, carga) "
                + "VALUES (:id, :tipo, :llave, '{}'::jsonb) RETURNING secuencia")
            .bind("id", UUID.randomUUID()).bind("tipo", tipo).bind("llave", pacienteId.toString())
            .map((row, metadata) -> row.get("secuencia", Long.class)).one();
    }

    protected void verificarAuditoriaInmutable(Mono<Long> operacion) {
        StepVerifier.create(operacion)
            .expectErrorMatches(error -> error.getMessage().contains("auditoria es inmutable"))
            .verify();
    }

}