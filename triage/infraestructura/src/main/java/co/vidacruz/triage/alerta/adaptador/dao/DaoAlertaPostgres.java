package co.vidacruz.triage.alerta.adaptador.dao;

import co.vidacruz.triage.alerta.modelo.dto.AlertaEscalamiento;
import co.vidacruz.triage.alerta.modelo.dto.DatosAlertaPaciente;
import co.vidacruz.triage.alerta.puerto.dao.DaoAlerta;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class DaoAlertaPostgres implements DaoAlerta {
    private final DatabaseClient db;

    public DaoAlertaPostgres(DatabaseClient db) {
        this.db = db;
    }

    @Override
    public Mono<DatosAlertaPaciente> consultarPaciente(UUID idPaciente) {
        return db.sql("SELECT nombre_completo, fecha_registro FROM paciente WHERE id = :id")
                .bind("id", idPaciente)
                .map((fila, metadata) -> new DatosAlertaPaciente(fila.get("nombre_completo", String.class),
                        fila.get("fecha_registro", Instant.class)))
                .one();
    }

    @Override
    public Flux<AlertaEscalamiento> consultarEscaladosActivos(Instant ahora) {
        String consulta = """
                SELECT p.id, p.nombre_completo, p.nivel, p.fecha_registro, a.ocurrido_en
                FROM paciente p
                JOIN auditoria a ON a.paciente_id = p.id AND a.tipo_evento = 'paciente_escalado'
                WHERE p.escalado = true AND p.estado <> 'ALTA'
                ORDER BY a.ocurrido_en, p.id
                """;
        return db.sql(consulta)
                .map((fila, metadata) -> {
                    Instant fechaRegistro = fila.get("fecha_registro", Instant.class);
                    return new AlertaEscalamiento(fila.get("id", UUID.class),
                            fila.get("nombre_completo", String.class), fila.get("nivel", String.class),
                            Math.max(0, Duration.between(fechaRegistro, ahora).toMinutes()),
                            fila.get("ocurrido_en", Instant.class));
                })
                .all();
    }
}
