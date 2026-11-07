package co.vidacruz.triage.auditoria.adaptador.repositorio;

import co.vidacruz.triage.auditoria.modelo.entidad.RegistroAuditoria;
import co.vidacruz.triage.auditoria.puerto.repositorio.RepositorioAuditoria;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class AdaptadorR2dbcAuditoria implements RepositorioAuditoria {
    private final DatabaseClient db;
    public AdaptadorR2dbcAuditoria(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<Void> registrar(RegistroAuditoria registro) {
        String insercion = """
                INSERT INTO auditoria (
                    id, paciente_id, tipo_evento, estado_anterior, estado_nuevo, ocurrido_en
                ) VALUES (:id, :paciente, :tipo, :anterior, :nuevo, :ocurrido)
                """;
        var consulta = db.sql(insercion)
                .bind("id", UUID.randomUUID()).bind("paciente", registro.pacienteId()).bind("tipo", registro.tipoEvento())
                .bind("nuevo", registro.estadoNuevo()).bind("ocurrido", registro.ocurridoEn());
        if (registro.estadoAnterior() == null) consulta = consulta.bindNull("anterior", String.class);
        else consulta = consulta.bind("anterior", registro.estadoAnterior());
        return consulta.fetch().rowsUpdated().then();
    }
}
