package co.vidacruz.triage.paciente.adaptador.dao;

import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import co.vidacruz.triage.paciente.puerto.dao.DaoPaciente;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class DaoPacientePostgres implements DaoPaciente {
    private final DatabaseClient db;
    private final MapeoPaciente mapeo = new MapeoPaciente();
    public DaoPacientePostgres(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<DtoPaciente> consultar(UUID id, Instant ahora) {
        String consulta = """
                SELECT id, nombre_completo, edad, frecuencia_cardiaca, presion_sistolica,
                       presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal,
                       nivel, sla_minutos, estado, escalado, fecha_registro, fecha_estado,
                       fecha_primera_consulta
                FROM paciente
                WHERE id = :id
                """;
        return db.sql(consulta)
                .bind("id", id).map((fila, metadata) -> mapeo.mapear(fila, ahora)).one();
    }
}
