package co.vidacruz.triage.alerta.modelo.dto;

import java.time.Instant;
import java.util.UUID;

public record AlertaEscalamiento(UUID idPaciente, String nombre, String nivel, long minutosEsperando,
        Instant escaladoEn) { }
