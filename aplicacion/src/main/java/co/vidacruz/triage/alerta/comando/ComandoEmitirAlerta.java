package co.vidacruz.triage.alerta.comando;

import java.time.Instant;
import java.util.UUID;

public record ComandoEmitirAlerta(UUID idEvento, UUID idPaciente, String nivel, Instant escaladoEn) { }
