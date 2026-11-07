# calidad-transversal

## NUEVO

### Requirement: Calidad reactiva y guía de operación de la entrega
Las capas permanecen separadas, dominio no depende de Spring/R2DBC/Kafka, y los flujos reactivos no bloquean el event loop. Integración usa WebTestClient y Testcontainers PostgreSQL/Kafka, BlockHound y ArquitecturaTest; cobertura mínima 80 %. README documenta cómo probar SLA, intervalo, SSE y reporte, variables nuevas y limitación de EventSource.

**Origen en el RFC:** CA-29, CA-30

#### Scenario: Flujos cumplen calidad transversal
**Origen en el RFC:** CA-29
- **WHEN** se ejecutan los flujos contra PostgreSQL y Kafka reales con BlockHound activo
- **THEN** no se detectan llamadas bloqueantes, ArquitecturaTest permanece verde y cobertura alcanza al menos 80 %

#### Scenario: Instrucciones de prueba disponibles
**Origen en el RFC:** CA-30
- **WHEN** se consulta el README de Triage
- **THEN** explica cómo configurar SLA/intervalo, ejecutar curl SSE y reporte, la limitación EventSource y las variables nuevas
