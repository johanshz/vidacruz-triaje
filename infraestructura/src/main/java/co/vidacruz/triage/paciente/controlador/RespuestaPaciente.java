package co.vidacruz.triage.paciente.controlador;

import co.vidacruz.triage.paciente.modelo.dto.DtoPaciente;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;

public record RespuestaPaciente(UUID id, String nombreCompleto, int edad, int frecuenciaCardiaca, int presionSistolica,
        int presionDiastolica, int spo2, BigDecimal temperatura, int dolor, int gcs, String quejaPrincipal,
        NivelTriage nivel, int slaMinutos, EstadoPaciente estado, boolean escalado, Instant fechaRegistro,
        Instant fechaEstado, Instant fechaPrimeraConsulta, long tiempoEnEstadoActualMinutos) {
    public static RespuestaPaciente desde(DtoPaciente dto) {
        return new RespuestaPaciente(dto.id(), dto.nombreCompleto(), dto.edad(), dto.frecuenciaCardiaca(),
                dto.presionSistolica(), dto.presionDiastolica(), dto.spo2(), dto.temperatura(), dto.dolor(), dto.gcs(),
                dto.quejaPrincipal(), dto.nivel(), dto.slaMinutos(), dto.estado(), dto.escalado(), dto.fechaRegistro(),
                dto.fechaEstado(), dto.fechaPrimeraConsulta(), dto.tiempoEnEstadoActualMinutos());
    }
}
