# consulta-protocolos

## NUEVO

### Requirement: Consultar protocolos clínicos con barreras anti-alucinación
`POST /asistente/consulta` recibe una pregunta de 3 a 500 caracteres tras recortar espacios. Con fragmentos relevantes, genera respuesta JSON usando solo el contexto; acepta únicamente citas recuperadas y existentes, construye fuentes únicas desde los metadatos y valida todas las cifras de la respuesta contra los fragmentos. Sin coincidencia o ante fallo de las barreras de calidad devuelve el aviso fijo y fuentes vacías sin exponer una respuesta no validada. Sin fragmentos no llama al LLM. JSON o fuentes inválidas no cuentan como fallos del circuito. La llamada a Claude no envía `thinking` desactivado ni `temperature`, opciones rechazadas por el modelo. La ruta exige `X-API-Key`; los errores usan ProblemDetail conforme al contrato RFC.

**Origen en el RFC:** F-03, RN-01, RN-02, RN-03, RN-04, RN-06, RN-07, RN-08, RN-10, CA-01 a CA-09, CA-14 a CA-17

#### Scenario: Responder las consultas clínicas con fuentes recuperadas
**Origen en el RFC:** CA-01, CA-02
- **WHEN** se consulta por las medidas iniciales de crisis hipertensiva o el primer antibiótico en sepsis y el índice recupera el protocolo correspondiente
- **THEN** responde 200 con una respuesta y fuente real del protocolo y sección recuperados

#### Scenario: Construir las citas desde metadatos y quitar duplicados
**Origen en el RFC:** CA-03
- **WHEN** el LLM devuelve citas repetidas o datos de cita distintos a los metadatos
- **THEN** la respuesta contiene cada fuente una sola vez usando los metadatos recuperados

#### Scenario: Degradar sin llamar al LLM cuando no hay coincidencia
**Origen en el RFC:** CA-04
- **WHEN** la pregunta no recupera fragmentos por encima del umbral
- **THEN** responde 200 con el aviso fijo y `fuentes: []`, sin llamar al puerto del chat

#### Scenario: Rechazar citas inventadas
**Origen en el RFC:** CA-05
- **WHEN** el LLM cita un protocolo o sección inexistente o no recuperado
- **THEN** responde 200 con el aviso fijo y `fuentes: []`

#### Scenario: Degradar ante falta de cobertura o JSON inválido
**Origen en el RFC:** CA-06
- **WHEN** el LLM marca `sinCobertura` o devuelve JSON inválido
- **THEN** responde 200 con el aviso fijo y esas respuestas no cuentan como fallo del circuito

#### Scenario: Rechazar cifras clínicas no respaldadas
**Origen en el RFC:** CA-07, CA-09
- **WHEN** la respuesta contiene una cifra que no aparece en los fragmentos, aunque aparezca en la pregunta
- **THEN** responde 200 con el aviso fijo y `fuentes: []`; la alerta no registra el texto clínico

#### Scenario: Aceptar cifras equivalentes y excluir identificadores
**Origen en el RFC:** CA-08
- **WHEN** la respuesta repite cifras respaldadas, incluidos rangos, razones y equivalencia `5`/`5.0`
- **THEN** pasa la validación numérica, que ignora identificadores de protocolo y numeración de listas

#### Scenario: Validar límites de la pregunta
**Origen en el RFC:** CA-14, CA-15
- **WHEN** la pregunta es ausente, vacía, solo espacios, menor de 3 o mayor de 500 caracteres, o tiene exactamente 3 o 500 caracteres tras recortar
- **THEN** las inválidas responden 400 `VALIDACION` con `campo=pregunta` y las preguntas dentro de límites no son rechazadas por longitud

#### Scenario: Rechazar cuerpo JSON mal formado o de tipo incorrecto
**Origen en el RFC:** CA-16
- **WHEN** el cuerpo no es JSON válido o `pregunta` no es texto
- **THEN** responde 400 `VALIDACION`

#### Scenario: Exigir API key en la consulta
**Origen en el RFC:** CA-17
- **WHEN** se omite `X-API-Key` o se envía una clave errada
- **THEN** responde 401 `NO_AUTORIZADO` y el LLM no recibe llamadas

### Requirement: Degradación controlada ante indisponibilidad del proveedor
La llamada a Claude se realiza por el puerto `AsistenteProtocolos`; fallos de conexión, timeout, 5xx, 429, 401 y 403 se traducen a 503 `IA_NO_DISPONIBLE` con el detalle definido por RFC, `traceId` y sin `Retry-After`. Se aplica TimeLimiter y CircuitBreaker configurables, sin reintentos. La clave Anthropic es opcional al arranque; su ausencia solo impide completar consultas que requieren LLM. La vista de urgencias y readiness permanecen disponibles.

**Origen en el RFC:** F-03, RN-05, RN-08, CA-10 a CA-13

#### Scenario: Rechazar rápidamente una consulta con circuito abierto
**Origen en el RFC:** CA-10
- **WHEN** se consulta con fragmentos disponibles y el circuito abierto
- **THEN** responde en menos de dos segundos con 503 `IA_NO_DISPONIBLE`, ProblemDetail, `traceId` y sin `Retry-After`

#### Scenario: Abrir circuito tras fallos del proveedor
**Origen en el RFC:** CA-11
- **WHEN** al menos cinco llamadas fallan en la ventana configurada hasta alcanzar el umbral del 50 %
- **THEN** cada fallo se traduce a 503 y el circuito abierto deja de invocar al modelo

#### Scenario: Operar sin clave Anthropic según la ruta consultada
**Origen en el RFC:** CA-12
- **WHEN** la aplicación arranca sin `ANTHROPIC_API_KEY` y se consulta una pregunta con protocolo, una sin protocolo y el estado de urgencias
- **THEN** la consulta con protocolo responde 503, la consulta sin protocolo responde 200 con el aviso fijo y el estado de urgencias responde 200

#### Scenario: Mantener disponibles urgencias y readiness con el circuito abierto
**Origen en el RFC:** CA-13
- **WHEN** el circuito del LLM está abierto
- **THEN** `GET /asistente/estado-urgencias` y `/actuator/health/readiness` responden 200

### Requirement: Comparar los modelos de Anthropic
La comparación opcional ejecuta las tres consultas de ejemplo con Sonnet 5.5 y Haiku 5.5 cuando existe clave real, y registra respuesta, fuentes, resultado de las cinco barreras, latencia, costo estimado y decisión. Haiku reemplaza a Sonnet solo si pasa las tres consultas y las cinco barreras igual que Sonnet. Sin clave o red deja una plantilla pendiente de ejecutar y un pendiente asignado a Johan; el gasto máximo es 5 USD.

**Origen en el RFC:** F-02, CA-25

#### Scenario: Registrar comparación de modelos o dejarla pendiente
**Origen en el RFC:** CA-25
- **WHEN** se cierra la fila con acceso a Anthropic o sin él
- **THEN** `comparacion-modelos.md` contiene los resultados y decisión por modelo y consulta, o una plantilla pendiente de ejecutar con un PE para Johan
