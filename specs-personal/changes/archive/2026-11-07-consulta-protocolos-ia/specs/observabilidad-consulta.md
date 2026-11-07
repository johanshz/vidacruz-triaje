# observabilidad-consulta

## NUEVO

### Requirement: Medir consultas y proteger el texto clínico en logs
La consulta publica el timer `asistente.llm.latencia` con `resultado=ok|error|circuito_abierto` y el contador `asistente.consultas` con `resultado=respondida|sin_cobertura|barrera_rechazada|no_disponible`. Las métricas se exponen en `/actuator/prometheus`. Los logs solo registran longitud, protocolos citados y resultado; nunca la pregunta ni la respuesta.

**Origen en el RFC:** F-04, RN-09, CA-20, CA-21

#### Scenario: Exponer métricas de consultas y llamadas al LLM
**Origen en el RFC:** CA-20
- **WHEN** ocurren resultados respondida, sin cobertura, rechazada por barrera y no disponible
- **THEN** Prometheus expone cada resultado del contador y las etiquetas de resultado del timer

#### Scenario: No registrar contenido clínico
**Origen en el RFC:** CA-21
- **WHEN** se procesa una pregunta con texto reconocible y se genera una respuesta
- **THEN** los logs no contienen la pregunta ni la respuesta y solo registran longitud, protocolos citados y resultado
