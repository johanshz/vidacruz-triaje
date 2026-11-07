# calidad-transversal

## NUEVO

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
