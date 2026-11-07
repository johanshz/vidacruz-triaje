package co.vidacruz.triage.paciente.modelo.dto;

import co.vidacruz.triage.paciente.modelo.entidad.EstadoPaciente;
import co.vidacruz.triage.paciente.modelo.entidad.NivelTriage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DtoPaciente(UUID id, String nombreCompleto, int edad, int frecuenciaCardiaca, int presionSistolica,
        int presionDiastolica, int spo2, BigDecimal temperatura, int dolor, int gcs, String quejaPrincipal,
        NivelTriage nivel, int slaMinutos, EstadoPaciente estado, boolean escalado, Instant fechaRegistro,
        Instant fechaEstado, Instant fechaPrimeraConsulta, long tiempoEnEstadoActualMinutos) { }
