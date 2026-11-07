package co.vidacruz.triage.paciente.adaptador.repositorio;

import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class AdaptadorR2dbcPaciente implements RepositorioPaciente {
    private final DatabaseClient db;

    public AdaptadorR2dbcPaciente(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<Void> guardar(Paciente p) {
        var d = p.datos();
        return db.sql("INSERT INTO paciente (id,nombre_completo,edad,frecuencia_cardiaca,presion_sistolica,presion_diastolica,spo2,temperatura,dolor,gcs,queja_principal,nivel,sla_minutos,estado,escalado,fecha_registro,fecha_estado,fecha_primera_consulta) VALUES (:id,:nombre,:edad,:fc,:ps,:pd,:spo2,:temp,:dolor,:gcs,:queja,:nivel,:sla,:estado,:escalado,:registro,:fechaEstado,:primera)")
                .bind("id", p.id()).bind("nombre", d.nombreCompleto()).bind("edad", (short) d.edad())
                .bind("fc", (short) d.frecuenciaCardiaca()).bind("ps", (short) d.presionSistolica()).bind("pd", (short) d.presionDiastolica())
                .bind("spo2", (short) d.spo2()).bind("temp", d.temperatura()).bind("dolor", (short) d.dolor()).bind("gcs", (short) d.gcs())
                .bind("queja", d.quejaPrincipal()).bind("nivel", p.nivel().name()).bind("sla", p.nivel().slaMinutos())
                .bind("estado", p.estado().name()).bind("escalado", p.escalado()).bind("registro", p.fechaRegistro())
                .bind("fechaEstado", p.fechaEstado()).bindNull("primera", java.time.Instant.class).fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Boolean> cambiarEstado(UUID id, Paciente p, String estadoAnterior) {
        var consulta = db.sql("UPDATE paciente SET estado=:estado, fecha_estado=:fechaEstado, fecha_primera_consulta=:primera WHERE id=:id AND estado=:anterior")
                .bind("estado", p.estado().name()).bind("fechaEstado", p.fechaEstado()).bind("id", id).bind("anterior", estadoAnterior);
        if (p.fechaPrimeraConsulta() == null) consulta = consulta.bindNull("primera", java.time.Instant.class);
        else consulta = consulta.bind("primera", p.fechaPrimeraConsulta());
        return consulta.fetch().rowsUpdated().map(actualizadas -> actualizadas == 1);
    }
}
