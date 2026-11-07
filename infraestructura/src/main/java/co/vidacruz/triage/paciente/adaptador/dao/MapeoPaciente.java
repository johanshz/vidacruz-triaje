package co.vidacruz.triage.paciente.adaptador.dao;

import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import io.r2dbc.spi.Row;
import java.time.Duration;
import java.time.Instant;

public class MapeoPaciente {
    public DtoPaciente mapear(Row fila, Instant ahora) {
        Instant fechaEstado = fila.get("fecha_estado", Instant.class);
        return new DtoPaciente(fila.get("id", java.util.UUID.class), fila.get("nombre_completo", String.class),
                fila.get("edad", Short.class).intValue(), fila.get("frecuencia_cardiaca", Short.class).intValue(),
                fila.get("presion_sistolica", Short.class).intValue(), fila.get("presion_diastolica", Short.class).intValue(),
                fila.get("spo2", Short.class).intValue(), fila.get("temperatura", java.math.BigDecimal.class),
                fila.get("dolor", Short.class).intValue(), fila.get("gcs", Short.class).intValue(),
                fila.get("queja_principal", String.class), NivelTriage.valueOf(fila.get("nivel", String.class)),
                fila.get("sla_minutos", Integer.class), EstadoPaciente.valueOf(fila.get("estado", String.class)),
                fila.get("escalado", Boolean.class), fila.get("fecha_registro", Instant.class), fechaEstado,
                fila.get("fecha_primera_consulta", Instant.class), Math.max(0, Duration.between(fechaEstado, ahora).toMinutes()));
    }
}
