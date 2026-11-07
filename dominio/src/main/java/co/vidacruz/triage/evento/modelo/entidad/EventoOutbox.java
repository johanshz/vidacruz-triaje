package co.vidacruz.triage.evento.modelo.entidad;

import java.util.UUID;

public record EventoOutbox(UUID id, long secuencia, String tipo, UUID idPaciente, String carga) { }
