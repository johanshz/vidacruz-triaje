# consulta-protocolos-ia

## Por qué
El médico necesita consultar protocolos clínicos oficiales en lenguaje natural. El Asistente debe responder con citas verificables y degradar de forma clara ante falta de cobertura o indisponibilidad de Claude, sin inventar datos ni afectar la vista de urgencias.

## Qué cambia
- F-01: carga el corpus oficial y crea un índice vectorial en memoria con embeddings locales.
- F-02: compara Sonnet 5.5 y Haiku 5.5 con las tres consultas de ejemplo y deja un informe o una plantilla pendiente.
- F-03: agrega `POST /asistente/consulta`, validación, cinco barreras anti-alucinación y circuit breaker con respuesta 503.
- F-04: agrega métricas y logs sin texto clínico.
- F-05: amplía la puerta de calidad y las pruebas de la fila, manteniendo JaCoCo, BlockHound y ArchUnit.

## Qué NO cambia
No cambia Triage ni la vista de urgencias existente. No se agregan protocolos al corpus, persistencia nueva, R2DBC, despliegue Kubernetes/Azure ni exportación OTLP. El Asistente no llama al Triage.

## RFC de origen
`desarrollo-ia/asistente/rfc-activo.md`
