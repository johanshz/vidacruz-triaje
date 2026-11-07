-- Rastro W3C (traceparent) de la petición que creó el evento. Se envía como cabecera en Kafka
-- para que el Asistente continúe la misma traza (DP13). NULL cuando el evento no nace de una petición.
ALTER TABLE outbox ADD COLUMN traceparent VARCHAR(55);
