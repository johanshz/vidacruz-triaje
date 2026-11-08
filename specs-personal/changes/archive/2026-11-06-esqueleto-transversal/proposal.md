# esqueleto-transversal

## Por qué
El Triage tiene únicamente el estructura hexagonal con CQRS del proyecto y necesita quedar listo para las siguientes filas funcionales, con esquema persistente, seguridad, manejo uniforme de errores, observabilidad, entorno local y una puerta de calidad efectiva.

## Qué cambia
- F-01: migraciones Flyway para paciente, auditoría inmutable y outbox, y configuración de conexión por variables de entorno.
- F-02: autenticación con `X-API-Key` en todas las rutas salvo health.
- F-03: errores HTTP en ProblemDetail con códigos de negocio y `traceId`.
- F-04: probes liveness/readiness, readiness dependiente de R2DBC y endpoints actuator.
- F-05: logs ECS, tracing local y cabecera `X-Trace-Id`.
- F-06: imagen Docker, compose local y `.env.example`.
- F-07: JaCoCo, BlockHound y ArchUnit aplicado a clases reales.

## Qué NO cambia
No agrega endpoints de negocio, Kafka al código del Triage, Redis al código, exportación OTLP/Jaeger, manifiestos de Kubernetes ni integración con Asistente.

## RFC de origen
desarrollo-ia/triage/rfc-activo.md
