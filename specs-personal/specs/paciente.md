# paciente

### Requirement: Registro y clasificación transaccional
El endpoint registra pacientes válidos, aplica clasificación de triaje y SLA, y persiste ficha, auditoría y evento outbox en una única transacción. La clasificación evalúa gravedad de nivel 1 a 5; la validación de tipos y rangos se realiza en dominio.

**Origen en el RFC:** F-01, RN-01, RN-02, RN-03, RN-04, RN-05, CA-01 a CA-11

#### Scenario: Pacientes de ejemplo y límites de clasificación
**Origen en el RFC:** CA-01, CA-03
- **WHEN** se registran los seis ejemplos Ceiba y pacientes con los valores frontera definidos para signos vitales, dolor y GCS
- **THEN** cada uno recibe el nivel y SLA que corresponden a la regla RN-01, prevalece el nivel más grave y el endpoint devuelve 201

#### Scenario: Clasificación administrativa
**Origen en el RFC:** CA-04
- **WHEN** una queja coincide exactamente tras normalizar con una queja administrativa y los signos producirían `NO_URGENTE`
- **THEN** el paciente recibe `NO_URGENTE_2` y SLA 120; quejas no iguales o signos de nivel más grave conservan la clasificación ordinaria

#### Scenario: Rangos, límites y validación del request
**Origen en el RFC:** CA-05, CA-06, CA-07, CA-08
- **WHEN** el request contiene un campo fuera de rango, tipo incorrecto, dato obligatorio ausente, temperatura con más de un decimal, presión diastólica no menor que sistólica, nombre/queja inválidos o JSON mal formado
- **THEN** responde 400 `VALIDACION` con el campo cuando sea identificable y no persiste datos; los límites válidos y textos de longitud admitida responden 201

#### Scenario: Persistencia atómica del registro y métrica
**Origen en el RFC:** CA-02, CA-09, CA-10
- **WHEN** se registra un paciente válido y, en una ejecución separada, falla la escritura del evento outbox
- **THEN** el éxito crea ficha, auditoría y outbox juntos, después de confirmar la transacción incrementa una sola vez la métrica por nivel; el fallo responde 500 `ERROR_INTERNO` y no deja ficha ni auditoría
- **AND** un registro rechazado con 400 no incrementa ningún contador

#### Scenario: Protección por API key
**Origen en el RFC:** CA-11
- **WHEN** se llama a cualquiera de los endpoints de paciente sin `X-API-Key` válida
- **THEN** responde 401 `NO_AUTORIZADO` y no escribe datos

### Requirement: Consulta directa de la ficha del paciente
La consulta obtiene los datos mediante `DaoPaciente` y MapRow sin pasar por entidad ni servicio. Devuelve ficha completa, estado, `escalado`, fechas y minutos enteros truncados desde `fecha_estado`; id UUID ausente e id mal formado generan los errores definidos.

**Origen en el RFC:** F-02, RN-13, CA-12, CA-13, CA-14

#### Scenario: Ficha existente y tiempo en estado
**Origen en el RFC:** CA-12, CA-14
- **WHEN** se consulta un paciente con reloj controlado, incluido uno que cambió de estado
- **THEN** responde 200 con sus datos, `escalado = false`, fechas correctas y minutos enteros desde la fecha del estado vigente

#### Scenario: UUID inexistente o inválido
**Origen en el RFC:** CA-13
- **WHEN** se consulta un UUID inexistente o un identificador no UUID
- **THEN** responde 404 `NO_ENCONTRADO` o 400 `VALIDACION` con campo `id`, respectivamente
