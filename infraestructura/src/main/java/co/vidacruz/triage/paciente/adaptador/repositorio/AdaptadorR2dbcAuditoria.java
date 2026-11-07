package co.vidacruz.triage.paciente.adaptador.repositorio;

import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioAuditoria;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class AdaptadorR2dbcAuditoria implements RepositorioAuditoria {
    private final DatabaseClient db;
    public AdaptadorR2dbcAuditoria(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<Void> registrar(Paciente paciente, String tipoEvento, String estadoAnterior, String estadoNuevo, Instant ocurridoEn) {
        var consulta = db.sql("INSERT INTO auditoria (id,paciente_id,tipo_evento,estado_anterior,estado_nuevo,ocurrido_en) VALUES (:id,:paciente,:tipo,:anterior,:nuevo,:ocurrido)")
                .bind("id", UUID.randomUUID()).bind("paciente", paciente.id()).bind("tipo", tipoEvento)
                .bind("nuevo", estadoNuevo).bind("ocurrido", ocurridoEn);
        if (estadoAnterior == null) consulta = consulta.bindNull("anterior", String.class);
        else consulta = consulta.bind("anterior", estadoAnterior);
        return consulta.fetch().rowsUpdated().then();
    }
}
