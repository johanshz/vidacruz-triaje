# observabilidad

## NUEVO

### Requirement: Correlación de peticiones y errores
Cada respuesta HTTP lleva `X-Trace-Id` correspondiente al span actual. Los logs de consola usan formato ECS e incluyen `traceId` y `spanId`; los ProblemDetail incluyen el mismo `traceId`.

**Origen en el RFC:** F-05, RN-04, CA-23, CA-24

#### Scenario: Respuesta HTTP correlacionada
**Origen en el RFC:** CA-23
- **WHEN** se procesa una petición HTTP
- **THEN** la respuesta incluye `X-Trace-Id` con el identificador del span

#### Scenario: Log estructurado
**Origen en el RFC:** CA-24
- **WHEN** se procesa una petición
- **THEN** el log de consola es ECS e incluye `traceId` y `spanId`

### Requirement: Probes y health de infraestructura
Actuator expone health, info y prometheus; liveness evalúa el estado de vida, readiness evalúa readiness y R2DBC, y no incorpora Kafka ni Redis. Los detalles de health no se exponen.

**Origen en el RFC:** F-04, RN-05, CA-21, CA-22

#### Scenario: Readiness refleja disponibilidad de PostgreSQL
**Origen en el RFC:** CA-21
- **WHEN** R2DBC no puede responder
- **THEN** readiness queda DOWN

#### Scenario: Health no revela detalles
**Origen en el RFC:** CA-22
- **WHEN** se consulta cualquier probe de health
- **THEN** la respuesta no expone detalles internos
