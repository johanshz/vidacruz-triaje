# ADR-05 — Alertas de escalamiento al equipo médico (RF-06)

- **Fecha:** 2026-11-06
- **Estado:** Aceptada

## Contexto
RF-06 está abierto a propósito: alertar al equipo médico cuando un paciente es escalado, con
canal, formato y tecnología a elección. Es backend sin frontend; el evaluador debe poder verlo
funcionar. Hay 2 réplicas del Triage.

## Supuestos
- El equipo médico tiene una pantalla o cliente en la sala que puede mantener una conexión HTTP abierta.
- La alerta debe llegar en segundos, no en minutos; no se exige confirmación de lectura.
- Una alerta perdida mientras nadie está conectado se recupera al conectarse (estado inicial).

## Decisión
- El módulo `alerta` del Triage consume `paciente_escalado` de `triaje.eventos`. Cada réplica
  usa su propio grupo (`triage-alertas-<instancia>`, desde lo último), así todas reciben todas las alertas.
- Cada réplica emite la alerta a un `Sinks.Many` multicast y la expone por **Server-Sent Events**
  en `GET /triage/alertas/flujo` (`text/event-stream`, protegido con `X-API-Key`).
- Al conectarse, el cliente recibe primero los pacientes activos ya escalados y luego cada alerta nueva:
  `{ "idPaciente", "nombre", "nivel", "minutosEsperando", "escaladoEn" }`.
- Además: log estructurado `ALERTA_ESCALAMIENTO` y métrica `triaje.escalamientos`.
- El canal es un puerto (`NotificadorAlerta`): Teams, correo o SMS serían otro adaptador.

## Opciones descartadas
| Opción | Por qué no |
|---|---|
| Webhook a Teams o Slack | Depende de una cuenta externa que el evaluador no tiene; difícil de probar |
| Correo | Lento para una urgencia y requiere servidor SMTP |
| Solo un topic de alertas en Kafka | Ningún médico lee Kafka; mueve el problema sin resolverlo |
| Servicio de Notificaciones aparte | Ver ADR-01 |

## Consecuencias
- Se prueba con `curl -N -H "X-API-Key: ..." <url>/triage/alertas/flujo` y bajando el SLA en propiedades.
- Tiempo real y reactivo de punta a punta (Kafka → `Sinks` → SSE), con contrapresión del propio flujo.
- Las conexiones SSE se reparten entre réplicas; por eso cada réplica consume todas las alertas.

## Cómo se defiende
"Elegí lo que el médico ve en segundos y el evaluador puede probar con un curl; el canal es un
puerto, así que Teams o correo son un adaptador más."
