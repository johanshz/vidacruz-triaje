package co.vidacruz.triage.paciente.adaptador.repositorio;

import co.vidacruz.triage.paciente.modelo.entidad.Paciente;
import co.vidacruz.triage.paciente.puerto.repositorio.RepositorioPaciente;
import co.vidacruz.triage.vigilancia.modelo.entidad.PacienteEscalado;
import co.vidacruz.triage.vigilancia.puerto.repositorio.RepositorioEscalamiento;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import java.time.Instant;
import java.util.UUID;
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
    public Mono<Void> guardar(Paciente paciente) {
        var datos = paciente.datos();
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
        var ejecucion = db.sql(insercion)
                .bind("id", paciente.id()).bind("nombre", datos.nombreCompleto()).bind("edad", (short) datos.edad())
                .bind("fc", (short) datos.frecuenciaCardiaca()).bind("ps", (short) datos.presionSistolica())
                .bind("pd", (short) datos.presionDiastolica()).bind("spo2", (short) datos.spo2())
                .bind("temp", datos.temperatura()).bind("dolor", (short) datos.dolor()).bind("gcs", (short) datos.gcs())
                .bind("queja", datos.quejaPrincipal()).bind("nivel", paciente.nivel().name()).bind("sla", paciente.slaMinutos())
                .bind("estado", paciente.estado().name()).bind("escalado", paciente.escalado()).bind("registro", paciente.fechaRegistro())
                .bind("fechaEstado", paciente.fechaEstado());
        var consulta = paciente.fechaPrimeraConsulta() == null
                ? ejecucion.bindNull("primera", Instant.class)
                : ejecucion.bind("primera", paciente.fechaPrimeraConsulta());
        return consulta.fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Boolean> cambiarEstado(Paciente paciente, EstadoPaciente estadoAnterior) {
        String actualizacion = """
                UPDATE paciente
                SET estado = :estado, fecha_estado = :fechaEstado,
                    fecha_primera_consulta = :primera
                WHERE id = :id AND estado = :anterior
                """;
        var consulta = db.sql(actualizacion)
                .bind("estado", paciente.estado().name()).bind("fechaEstado", paciente.fechaEstado())
                .bind("id", paciente.id()).bind("anterior", estadoAnterior.name());
        if (paciente.fechaPrimeraConsulta() == null) consulta = consulta.bindNull("primera", Instant.class);
        else consulta = consulta.bind("primera", paciente.fechaPrimeraConsulta());
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
