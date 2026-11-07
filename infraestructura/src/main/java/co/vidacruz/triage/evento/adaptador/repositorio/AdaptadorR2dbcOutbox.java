package co.vidacruz.triage.evento.adaptador.repositorio;

import co.vidacruz.triage.evento.modelo.entidad.EventoOutbox;
import co.vidacruz.triage.evento.modelo.entidad.EventoPaciente;
import co.vidacruz.triage.evento.puerto.repositorio.RepositorioOutbox;
import co.vidacruz.triage.evento.puerto.repositorio.CandadoPublicacion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import java.util.List;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class AdaptadorR2dbcOutbox implements RepositorioOutbox, CandadoPublicacion {
    private static final long CANDADO_PUBLICADOR = 2026110601L;
    private final DatabaseClient db;
    private final ObjectMapper mapper;
    public AdaptadorR2dbcOutbox(DatabaseClient db, ObjectMapper mapper) { this.db = db; this.mapper = mapper; }

    @Override
    public Mono<Void> agregar(EventoPaciente evento, String llave) {
        try {
            String carga = mapper.writeValueAsString(evento);
            return db.sql("INSERT INTO outbox (id,tipo,llave,carga) VALUES (:id,:tipo,:llave,CAST(:carga AS jsonb))")
                    .bind("id", evento.idEvento()).bind("tipo", evento.tipo()).bind("llave", llave)
                    .bind("carga", carga).fetch().rowsUpdated().then();
        } catch (JsonProcessingException error) {
            return Mono.error(error);
        }
    }

    @Override
    public Mono<List<EventoOutbox>> pendientes(int limite) {
        return db.sql("SELECT id,secuencia,tipo,llave,carga FROM outbox WHERE enviado_en IS NULL ORDER BY secuencia LIMIT :limite")
                .bind("limite", limite).map((fila, metadata) -> {
                    Object carga = fila.get("carga");
                    String json = carga instanceof Json jsonb ? jsonb.asString() : String.valueOf(carga);
                    return new EventoOutbox(fila.get("id", java.util.UUID.class), fila.get("secuencia", Long.class),
                            fila.get("tipo", String.class), java.util.UUID.fromString(fila.get("llave", String.class)), json);
                }).all().collectList();
    }

    @Override
    public Mono<Boolean> tomar() {
        return db.sql("SELECT pg_try_advisory_xact_lock(:clave) AS adquirido").bind("clave", CANDADO_PUBLICADOR)
                .map((fila, metadata) -> fila.get("adquirido", Boolean.class)).one();
    }

    @Override
    public Mono<Void> marcarEnviado(EventoOutbox evento) {
        return db.sql("UPDATE outbox SET enviado_en=now() WHERE id=:id AND enviado_en IS NULL")
                .bind("id", evento.id()).fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Long> contarPendientes() {
        return db.sql("SELECT COUNT(*) AS cantidad FROM outbox WHERE enviado_en IS NULL")
                .map((fila, metadata) -> fila.get("cantidad", Long.class)).one();
    }
}
