# estado-paciente

## NUEVO

### Requirement: Cambio de estado con transiciones y persistencia atómicas
El sistema acepta solo `REGISTRADO → EN_CONSULTA`, `REGISTRADO → ALTA` y `EN_CONSULTA → ALTA`. Cualquier otra transición, incluido repetir estado, responde 409 descriptivo. Cada éxito actualiza ficha, auditoría y outbox en la misma transacción; ante solicitudes simultáneas solo una gana.

**Origen en el RFC:** F-03, RN-05, RN-06, RN-07, RN-08, RN-09, CA-15 a CA-19

#### Scenario: Transiciones válidas, auditoría y fechas
**Origen en el RFC:** CA-15, CA-16
- **WHEN** el paciente recorre una transición válida, incluida entrada a consulta o alta directa
- **THEN** responde 200 con la ficha completa, registra auditoría y evento, actualiza `fecha_estado` y fija `fecha_primera_consulta` solo en la primera entrada a consulta

#### Scenario: Transición inválida o request inválido
**Origen en el RFC:** CA-17, CA-18
- **WHEN** se solicita una transición no permitida o se envía estado faltante/desconocido, JSON mal formado o id inválido/inexistente
- **THEN** responde 409 `TRANSICION_INVALIDA` con detalle descriptivo, 400 `VALIDACION` o 404 `NO_ENCONTRADO` según corresponda, sin alterar los datos para solicitudes rechazadas

#### Scenario: Dos cambios concurrentes
**Origen en el RFC:** CA-19
- **WHEN** dos cambios de estado incompatibles/idénticos compiten por el mismo paciente
- **THEN** una petición responde 200 y la otra 409, quedando una sola actualización, auditoría y evento
