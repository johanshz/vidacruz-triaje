# Índice — Asistente

Actualizado: 2026-11-06 — change `asistente-urgencias`

## Puntos de entrada vigentes
| Método y ruta (o job / evento) | Qué hace | Spec | Desde |
|---|---|---|---|
| Kafka `triaje.eventos`, consumidor `ConsumidorEventosTriaje` | Actualiza proyección de urgencias en Redis | vista-urgencias.md | 2026-11-06 asistente-urgencias |
| GET `/asistente/estado-urgencias` | Consulta conteos por nivel | vista-urgencias.md | 2026-11-06 asistente-urgencias |
| GET `/actuator/health/**` | Probes abiertas | plataforma-asistente.md | 2026-11-06 asistente-urgencias |
| GET `/actuator/info` | Información protegida | plataforma-asistente.md | 2026-11-06 asistente-urgencias |
| GET `/actuator/prometheus` | Métricas protegidas | plataforma-asistente.md | 2026-11-06 asistente-urgencias |

## Historial de entregas
| Fecha | Change | Nuevos | Modificados | Eliminados |
|---|---|---|---|---|
| 2026-11-06 | asistente-urgencias | Configuración de Kafka y Redis, Seguridad por API key, Errores HTTP uniformes, Probes, trazas y logs, Entorno local reproducible, ArchUnit verifica clases reales, Proyección de pacientes activos desde eventos DP3, Consulta del estado de urgencias, Puerta de calidad y límites reactivos | — | — |
