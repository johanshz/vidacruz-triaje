# Índice — Triage

Actualizado: 2026-11-07 — change `camino-principal-paciente`

## Puntos de entrada vigentes
| Método y ruta (o job / evento) | Qué hace | Spec | Desde |
|---|---|---|---|
| GET `/actuator/health/liveness` | Probe de vida pública | observabilidad.md | 2026-11-06 esqueleto-transversal |
| GET `/actuator/health/readiness` | Probe R2DBC pública | observabilidad.md | 2026-11-06 esqueleto-transversal |
| GET `/actuator/info` | Información protegida por API key | seguridad.md | 2026-11-06 esqueleto-transversal |
| GET `/actuator/prometheus` | Métricas protegidas por API key | seguridad.md | 2026-11-06 esqueleto-transversal |
| Manejador global de errores | Traduce errores HTTP a ProblemDetail | errores.md | 2026-11-06 esqueleto-transversal |
| Arranque de aplicación / Flyway | Aplica V1 antes de disponibilidad | esquema-persistencia.md | 2026-11-06 esqueleto-transversal |
| WebFilter HTTP | Agrega `X-Trace-Id` a toda respuesta | observabilidad.md | 2026-11-06 esqueleto-transversal |
| Docker Compose | Levanta dependencias y Triage local | entorno-calidad.md | 2026-11-06 esqueleto-transversal |
| `check` Gradle | Ejecuta arquitectura, BlockHound y umbral JaCoCo | entorno-calidad.md | 2026-11-06 esqueleto-transversal |
| POST `/triage/pacientes` | Registra, clasifica y persiste ficha, auditoría y outbox | paciente.md | 2026-11-07 camino-principal-paciente |
| GET `/triage/pacientes/{id}` | Consulta ficha mediante DAO/MapRow | paciente.md | 2026-11-07 camino-principal-paciente |
| PATCH `/triage/pacientes/{id}/estado` | Cambia estado con auditoría y outbox | estado-paciente.md | 2026-11-07 camino-principal-paciente |
| Job periódico del outbox | Publica eventos pendientes en Kafka | publicacion-eventos.md | 2026-11-07 camino-principal-paciente |

## Historial de entregas
| Fecha | Change | Nuevos | Modificados | Eliminados |
|---|---|---|---|---|
| 2026-11-06 | esqueleto-transversal | 7 requirements | — | — |
| 2026-11-07 | camino-principal-paciente | No bloqueo y límites de capas, Instrucciones reproducibles del camino principal, Cambio de estado con transiciones y persistencia atómicas, Registro y clasificación transaccional, Consulta directa de la ficha del paciente, Publicación ordenada y recuperable del outbox, Sobre de evento de paciente | — | — |
