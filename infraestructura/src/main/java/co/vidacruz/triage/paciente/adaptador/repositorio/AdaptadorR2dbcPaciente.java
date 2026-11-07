package co.vidacruz.triage.paciente.adaptador.repositorio;

import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import co.vidacruz.triage.vigilancia.modelo.entidad.PacienteEscalado;
import co.vidacruz.triage.vigilancia.puerto.repositorio.RepositorioEscalamiento;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Repository
public class AdaptadorR2dbcPaciente implements RepositorioPaciente, RepositorioEscalamiento {
    private final DatabaseClient db;

    public AdaptadorR2dbcPaciente(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<Void> guardar(Paciente p) {
        var d = p.datos();
        String insercion = """
                INSERT INTO paciente (
                    id, nombre_completo, edad, frecuencia_cardiaca, presion_sistolica,
                    presion_diastolica, spo2, temperatura, dolor, gcs, queja_principal,
                    nivel, sla_minutos, estado, escalado, fecha_registro, fecha_estado,
                    fecha_primera_consulta
                ) VALUES (
                    :id, :nombre, :edad, :fc, :ps, :pd, :spo2, :temp, :dolor, :gcs,
                    :queja, :nivel, :sla, :estado, :escalado, :registro, :fechaEstado,
                    :primera
                )
                """;
        return db.sql(insercion)
                .bind("id", p.id()).bind("nombre", d.nombreCompleto()).bind("edad", (short) d.edad())
                .bind("fc", (short) d.frecuenciaCardiaca()).bind("ps", (short) d.presionSistolica())
                .bind("pd", (short) d.presionDiastolica()).bind("spo2", (short) d.spo2())
                .bind("temp", d.temperatura()).bind("dolor", (short) d.dolor()).bind("gcs", (short) d.gcs())
                .bind("queja", d.quejaPrincipal()).bind("nivel", p.nivel().name()).bind("sla", p.slaMinutos())
                .bind("estado", p.estado().name()).bind("escalado", p.escalado()).bind("registro", p.fechaRegistro())
                .bind("fechaEstado", p.fechaEstado()).bindNull("primera", java.time.Instant.class).fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Boolean> cambiarEstado(UUID id, Paciente p, String estadoAnterior) {
        String actualizacion = """
                UPDATE paciente
                SET estado = :estado, fecha_estado = :fechaEstado,
                    fecha_primera_consulta = :primera
                WHERE id = :id AND estado = :anterior
                """;
        var consulta = db.sql(actualizacion)
                .bind("estado", p.estado().name()).bind("fechaEstado", p.fechaEstado()).bind("id", id).bind("anterior", estadoAnterior);
        if (p.fechaPrimeraConsulta() == null) consulta = consulta.bindNull("primera", java.time.Instant.class);
        else consulta = consulta.bind("primera", p.fechaPrimeraConsulta());
        return consulta.fetch().rowsUpdated().map(actualizadas -> actualizadas == 1);
    }

    @Override
    public Flux<PacienteEscalado> escalarVencidos(Instant ahora) {
        String actualizacion = """
                UPDATE paciente
                SET escalado = true
                WHERE estado = 'REGISTRADO' AND escalado = false
                  AND fecha_registro + (sla_minutos * INTERVAL '1 minute') <= :ahora
                RETURNING id, nivel
                """;
        return db.sql(actualizacion).bind("ahora", ahora).map((fila, metadata) ->
                new PacienteEscalado(fila.get("id", UUID.class),
                        NivelTriage.valueOf(fila.get("nivel", String.class)))).all();
    }
}
