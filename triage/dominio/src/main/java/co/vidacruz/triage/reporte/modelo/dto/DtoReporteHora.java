package co.vidacruz.triage.reporte.modelo.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public record DtoReporteHora(OffsetDateTime hora, int totalRegistrados, int acumuladoRegistrados,
        Map<String, Integer> porNivel, BigDecimal esperaPromedioMinutos, BigDecimal esperaP95Minutos) { }
