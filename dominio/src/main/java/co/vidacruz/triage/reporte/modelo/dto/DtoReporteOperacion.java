package co.vidacruz.triage.reporte.modelo.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record DtoReporteOperacion(OffsetDateTime desde, OffsetDateTime hasta, String zonaHoraria,
        List<DtoReporteHora> horas) { }
