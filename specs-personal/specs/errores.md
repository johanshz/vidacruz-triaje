# errores

### Requirement: Respuestas de error ProblemDetail
Los errores del servicio deben usar `application/problem+json` con `codigo`, `traceId`, tÃ­tulo, estado y detalle. Los 400 incluyen `campo` cuando pueda identificarse; los 500 usan mensaje genÃ©rico, no exponen la traza interna y se registran a nivel ERROR.

**Origen en el RFC:** RN-04, CA-14, CA-15, CA-16, CA-17, CA-18, CA-19, CA-20

#### Scenario: ValidaciÃ³n de negocio produce 400
**Origen en el RFC:** CA-14
- **WHEN** se lanza `ExcepcionValidacion` con campo `edad`
- **THEN** responde 400 con `VALIDACION`, el campo, el detalle y `traceId`

#### Scenario: JSON mal formado o tipo incorrecto
**Origen en el RFC:** CA-15
- **WHEN** se envÃ­a JSON mal formado o un decimal para un campo entero
- **THEN** responde 400 con `VALIDACION` y campo identificable cuando aplique

#### Scenario: Recurso ausente o ruta inexistente
**Origen en el RFC:** CA-16, CA-17
- **WHEN** una excepciÃ³n de recurso ausente o una ruta inexistente alcanza el manejador
- **THEN** responde 404 ProblemDetail con `NO_ENCONTRADO` y `traceId`

#### Scenario: Conflicto de negocio
**Origen en el RFC:** CA-18
- **WHEN** se lanza `ExcepcionConflicto`
- **THEN** responde 409 con `TRANSICION_INVALIDA` y `traceId`

#### Scenario: Otros errores 4xx de Spring
**Origen en el RFC:** CA-19
- **WHEN** una peticiÃ³n produce 405 o 415
- **THEN** conserva el status y responde ProblemDetail con `SOLICITUD_INVALIDA`

#### Scenario: ExcepciÃ³n no controlada
**Origen en el RFC:** CA-20
- **WHEN** ocurre una RuntimeException no manejada
- **THEN** responde 500 `ERROR_INTERNO` con detalle genÃ©rico y registra la excepciÃ³n sin devolver su traza en el cuerpo


Implementación de `campo` para errores JSON: se toma el último nombre de campo de la ruta Jackson asociada al fallo. Si no existe una ruta (por ejemplo, JSON mal formado), se permite `campo` nulo.
