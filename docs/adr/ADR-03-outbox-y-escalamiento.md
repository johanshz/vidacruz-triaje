# ADR-03 — Outbox transaccional y escalamiento idempotente

- **Fecha:** 2026-11-06
- **Estado:** Aceptada

## Contexto
Cada cambio del paciente debe quedar en PostgreSQL, en la auditoría (RN-04) y como evento en
Kafka. Si Kafka cae entre guardar y publicar, el evento se pierde y el Asistente y las alertas
quedan mal. Además, con 2 réplicas del Triage, el job de SLA corre dos veces a la vez y RN-03
prohíbe escalar dos veces.

## Decisión
1. **Outbox:** en una sola transacción R2DBC (`TransactionalOperator`) se escriben la ficha, la
   fila de auditoría y el evento en la tabla `outbox`. Un job publicador (cada 1 s, configurable)
   lee los pendientes en orden, los envía con `KafkaTemplate` (llave = `idPaciente`) y marca
   `enviado_en`. Entrega "al menos una vez"; los consumidores son idempotentes (ADR-02).
2. **Escalamiento:** el job de vigilancia (intervalo y SLA por nivel en propiedades) ejecuta
   ```sql
   UPDATE paciente SET escalado = true
    WHERE estado = 'REGISTRADO' AND escalado = false
      AND fecha_registro + make_interval(mins => sla_minutos) <= :ahora
   RETURNING id, nivel
   ```
   Solo las filas devueltas generan auditoría y evento `paciente_escalado`, en la misma
   transacción. PostgreSQL bloquea la fila: si dos réplicas compiten, una recibe la fila y la otra cero.

## Opciones descartadas
| Opción | Por qué no |
|---|---|
| Publicar a Kafka después de guardar | Si Kafka cae en ese instante, el evento se pierde sin rastro |
| ShedLock (candado para que corra una sola réplica) | Librería extra; igual hace falta el `UPDATE` condicional para no duplicar |
| Aviso con retraso al registrar | Kafka no tiene retraso nativo; habría que construirlo y es difícil de probar |

## Consecuencias
- Ningún evento se pierde aunque Kafka caiga; se publica cuando vuelve.
- Latencia extra de hasta ~1 s en los eventos y hasta un intervalo del job en el escalamiento.
- Una tabla y un job más en el Triage; métrica `triaje.outbox.pendientes` para vigilarlo.
- Test obligatorio: dos ejecuciones concurrentes del escalamiento → un solo evento y una sola auditoría.

## Cómo se defiende
"Ningún evento se pierde aunque Kafka caiga, y la base de datos arbitra el escalamiento entre
réplicas sin candados externos."
