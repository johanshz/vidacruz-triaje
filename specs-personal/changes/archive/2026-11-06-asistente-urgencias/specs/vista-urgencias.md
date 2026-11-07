# vista-urgencias

## NUEVO

### Requirement: Proyección de pacientes activos desde eventos DP3
El consumidor procesa eventos de versión 1 conocidos, mantiene conjuntos Redis por nivel y no llama directamente al Triage. Los conjuntos hacen idempotente el procesamiento. Los mensajes inválidos no reintentables se envían a DLT; los errores transitorios se reintentan tres veces con esperas de 1, 2 y 4 segundos antes de DLT.

**Origen en el RFC:** RN-01, RN-02, RN-03, RN-04, RN-05, RN-06, RN-07; CA-01 a CA-11

#### Scenario: Registrar paciente según contrato DP3
**Origen en el RFC:** CA-01
- **WHEN** se procesa un evento `paciente_registrado` versión 1 con nivel válido y llave Kafka igual a `idPaciente`
- **THEN** el identificador se agrega al conjunto `urgencias:nivel:<NIVEL>`

#### Scenario: Dar de alta al paciente en todos los niveles
**Origen en el RFC:** CA-02
- **WHEN** se procesa `paciente_alta` para un identificador existente
- **THEN** se elimina de los cinco conjuntos, sin exigir nivel en el evento

#### Scenario: Conservar el nivel durante consulta o escalamiento
**Origen en el RFC:** CA-03
- **WHEN** llega `paciente_en_consulta` o `paciente_escalado`
- **THEN** la vista y el conteo permanecen sin cambios

#### Scenario: Procesar registro repetido idempotentemente
**Origen en el RFC:** CA-04
- **WHEN** se recibe otra vez el mismo registro del paciente
- **THEN** el paciente aparece una sola vez en el conjunto

#### Scenario: Dar de alta a un paciente que no existe
**Origen en el RFC:** CA-05
- **WHEN** llega `paciente_alta` para un identificador ausente
- **THEN** el offset se confirma, no se publica a DLT y los conteos no cambian

#### Scenario: Enviar JSON mal formado a DLT sin reintentos
**Origen en el RFC:** CA-06
- **WHEN** el consumidor recibe contenido que no es JSON válido
- **THEN** publica el mensaje en `triaje.eventos.DLT` sin reintentos, registra WARN y continúa con mensajes posteriores

#### Scenario: Enviar eventos incompletos a DLT
**Origen en el RFC:** CA-07
- **WHEN** falta `tipo` o `datos.idPaciente`
- **THEN** cada mensaje va a DLT sin reintentos y queda registro WARN

#### Scenario: Enviar nivel inválido de registro a DLT
**Origen en el RFC:** CA-08
- **WHEN** un `paciente_registrado` tiene nivel nulo o fuera del catálogo
- **THEN** va a DLT sin reintentos y ningún conjunto cambia

#### Scenario: Ignorar tipo o versión desconocidos
**Origen en el RFC:** CA-09
- **WHEN** el tipo no está reconocido o la versión es distinta de 1
- **THEN** se registra WARN, no se altera Redis ni se publica a DLT y el offset avanza

#### Scenario: Reintentar errores transitorios del consumidor
**Origen en el RFC:** CA-10
- **WHEN** Redis falla al procesar un registro válido
- **THEN** se realizan tres reintentos con esperas crecientes y luego el mensaje queda en DLT

#### Scenario: Continuar la traza recibida de Triage
**Origen en el RFC:** CA-11
- **WHEN** un evento trae `traceparent` con un traceId conocido
- **THEN** el log del consumidor incluye ese traceId

### Requirement: Consulta del estado de urgencias
El endpoint `GET /asistente/estado-urgencias` devuelve los cinco niveles en orden fijo, con cero cuando no hay pacientes, total igual a la suma y hora UTC de consulta. No accede al Triage.

**Origen en el RFC:** RN-01, RN-04, RN-08; CA-12, CA-13, CA-14

#### Scenario: Consultar una vista vacía
**Origen en el RFC:** CA-12
- **WHEN** se consulta el estado con Redis vacío y API key válida
- **THEN** responde 200, cinco niveles ordenados en cero, total cero y `consultadoEn` ISO-8601 UTC

#### Scenario: Consultar conteos de pacientes activos
**Origen en el RFC:** CA-13
- **WHEN** existen los registros indicados y una baja de paciente
- **THEN** devuelve los conteos vigentes por nivel y el total como suma

#### Scenario: Manejar indisponibilidad de Redis en la consulta
**Origen en el RFC:** CA-14
- **WHEN** Redis falla o vence el tiempo de consulta
- **THEN** responde 503 ProblemDetail con `URGENCIAS_NO_DISPONIBLE` y traceId, nunca 500 por ese fallo

### Requirement: Puerta de calidad y límites reactivos
El sistema no bloquea el event loop y mantiene las dependencias entre capas. `check` exige pruebas con Testcontainers para Kafka y Redis, BlockHound y cobertura agregada de líneas mínima de 80 %, con las exclusiones indicadas en el RFC.

**Origen en el RFC:** RN-10; CA-27

#### Scenario: Validar calidad del cambio
**Origen en el RFC:** CA-27
- **WHEN** se ejecuta `./gradlew check`
- **THEN** pasan las pruebas requeridas y el build falla si la cobertura agregada es menor a 80 %
