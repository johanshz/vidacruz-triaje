package co.vidacruz.triage.evento.modelo.entidad;

import java.util.UUID;

/**
 * Evento pendiente del outbox. {@code traceparent} es el rastro W3C de la petición que lo creó
 * (puede ser {@code null}); viaja como cabecera de Kafka para continuar la traza en el Asistente.
 */
public record EventoOutbox(UUID id, long secuencia, String tipo, UUID idPaciente, String carga,
        String traceparent) { }
