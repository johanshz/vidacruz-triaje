# calidad-transversal

### Requirement: No bloqueo y límites de capas
Los flujos nuevos son reactivos, no bloquean el event loop y respetan la separación de capas de Ceiba; dominio no depende de Spring, R2DBC ni Kafka.

**Origen en el RFC:** F-01, F-02, F-03, F-04, CA-27

#### Scenario: Integración reactiva y arquitectura
**Origen en el RFC:** CA-27
- **WHEN** se ejecutan los flujos contra PostgreSQL y Kafka reales con BlockHound y ArquitecturaTest
- **THEN** no se detectan llamadas bloqueantes y las reglas de capas se mantienen

### Requirement: Instrucciones reproducibles del camino principal
El README explica cómo probar los escenarios principales de registro, consulta, cambio de estado y lectura de eventos, incluida la configuración de `KAFKA_BOOTSTRAP_SERVERS`.

**Origen en el RFC:** CA-28

#### Scenario: Prueba manual documentada
**Origen en el RFC:** CA-28
- **WHEN** una persona sigue la sección del README para el camino del paciente
- **THEN** puede ejecutar curls de E1 a E6, GET, PATCH válido e inválido, y observar el topic Kafka

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
