# Índice — triage

Actualizado: 2026-11-06 — change `esqueleto-transversal`

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

## Historial de entregas
| Fecha | Change | Nuevos | Modificados | Eliminados |
|---|---|---|---|---|
| 2026-11-06 | esqueleto-transversal | 7 requirements | — | — |
