# asistente-urgencias

## Por qué
El Asistente debe mantener una vista propia, reactiva y consultable del estado de urgencias, alimentada desde Kafka y Redis, con los controles transversales definidos en el RFC.

## Qué cambia
- F-01: conexión reactiva a Redis y consumo de Kafka con reintentos y DLT.
- F-02: autenticación por `X-API-Key` con probes de salud abiertas.
- F-03: errores HTTP uniformes en ProblemDetail.
- F-04: exposición de actuator y configuración de probes.
- F-05: trazas y logs estructurados con `traceId`.
- F-06: imagen Docker y entorno local con Redis, Kafka y Asistente.
- F-07: cobertura, BlockHound y reglas ArchUnit efectivas.
- F-08: actualización idempotente de la vista de urgencias desde eventos DP3.
- F-09: consulta HTTP de conteos por nivel.

## Qué NO cambia
No se implementan las funciones de IA de la fila 4, el productor Triage de la fila 2, el despliegue Kubernetes/Azure de la fila 6 ni la exportación OTLP de la fila 7. El servicio Asistente no crea el topic `triaje.eventos`; Docker Compose sí lo crea con 3 particiones para las pruebas locales, como exige F-06.

## RFC de origen
`desarrollo-ia/asistente/rfc-activo.md`
