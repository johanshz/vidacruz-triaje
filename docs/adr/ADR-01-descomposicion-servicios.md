# ADR-01 — Descomposición en dos microservicios

- **Fecha:** 2026-11-06
- **Estado:** Aceptada

## Contexto
El enunciado pide mínimo dos microservicios Java reactivos (Triage y Asistente Clínico) y deja a
criterio del candidato una descomposición adicional "justificada". Exige que el Triage nunca
dependa de la disponibilidad del Asistente. Prioridades: tolerancia a fallos, trazabilidad,
simplicidad; entrega en 3 días.

## Decisión
Dos servicios:
- **Triage:** pacientes (RF-01, RF-02), vigilancia de SLA (RF-03), auditoría (RN-04), outbox,
  reporte (RF-05) y alertas (RF-06).
- **Asistente:** protocolos con RAG (RF-04 A) y vista de urgencias (RF-04 B).

Se comunican **solo** por Kafka (`triaje.eventos`). Ninguno llama por HTTP al otro.

## Opciones descartadas
| Opción | Por qué no |
|---|---|
| Tercer servicio de Notificaciones (RF-06) | Ninguna razón de negocio lo exige (carga, equipo o tecnología distinta); suma un despliegue, una imagen y costo en Azure. Las alertas ya están desacopladas por Kafka dentro del Triage |
| Tercer servicio Monitor de SLA | Tendría que escribir el flag `escalado` en la ficha del paciente, que es del Triage: dos dueños para el mismo dato, o llamadas HTTP entre servicios (monolito repartido) |

## Consecuencias
- Aislamiento de fallos cumplido: si el Asistente o la IA caen, el Triage sigue (el enunciado lo pide).
- Cada servicio tiene su almacenamiento: PostgreSQL (Triage) y Redis (Asistente).
- Si las alertas crecen (varios canales, reintentos propios), el módulo `alerta` se extrae a un
  servicio sin tocar el resto: ya consume desde Kafka.

## Cómo se defiende
"Dos servicios cumplen el enunciado; un tercero no tenía razón de negocio y habría sumado costo
y tiempo."
