# publicacion-eventos

## NUEVO

### Requirement: Publicación ordenada y recuperable del outbox
El publicador reclama un candado consultivo PostgreSQL por ciclo para que opere una sola réplica, procesa eventos por `secuencia`, usa `idPaciente` como llave y marca `enviado_en` solo tras confirmación de Kafka. El productor usa `StringSerializer` para llave y valor, `acks=all`, idempotencia activada y envía como valor el JSON de `outbox.carga` sin transformarlo. Los ciclos de una misma instancia no se solapan y se omite el siguiente si el anterior no terminó. El job captura las excepciones para no caer: un fallo de Kafka genera log WARN con `idEvento`; uno de PostgreSQL genera log ERROR y se reintenta en el siguiente ciclo. La entrega es al menos una vez; los fallos dejan pendientes el evento fallido y los posteriores.

**Origen en el RFC:** F-04, RN-10, RN-11, RN-12, CA-20 a CA-26

#### Scenario: Publicación, llave, orden y carga original
**Origen en el RFC:** CA-20, CA-21
- **WHEN** hay eventos pendientes y Kafka confirma su recepción
- **THEN** se publican en orden de secuencia, con llave de paciente y valor igual al JSON de `outbox.carga` sin transformación, usando `StringSerializer`, `acks=all` e idempotencia activada; quedan marcados enviados y no se repiten en ciclos posteriores

### Requirement: Sobre de evento de paciente
Cada evento publicado conserva el contrato del sobre definido para los cambios de estado del paciente.

**Origen en el RFC:** F-04, CA-21, CA-15

#### Scenario: Sobre de registro y transiciones
**Origen en el RFC:** F-04, CA-21, CA-15
- **WHEN** se publican eventos de registro, entrada a consulta y alta
- **THEN** cada sobre tiene `idEvento` igual a `outbox.id`, `tipo` igual a `paciente_registrado`, `paciente_en_consulta` o `paciente_alta`, y `version = 1`; `ocurridoEn` está en ISO 8601 UTC y es igual a `auditoria.ocurrido_en`; `datos` contiene `idPaciente`, `nivel`, `escalado` y `estadoNuevo`; `datos.estadoAnterior` es nulo para `paciente_registrado` y contiene el estado previo para `paciente_en_consulta` y `paciente_alta`

#### Scenario: Topic de eventos
**Origen en el RFC:** CA-22
- **WHEN** arranca el Triage con Kafka disponible y el topic aún no existe
- **THEN** se crea `triaje.eventos` con tres particiones

#### Scenario: Kafka no disponible durante el arranque
**Origen en el RFC:** F-04, RN-12, CA-23
- **WHEN** Kafka está caído al arrancar el Triage
- **THEN** el servicio arranca sin que falle la creación del topic y readiness responde 200; no se exige reparar el topic tardíamente

#### Scenario: Kafka fuera de servicio y recuperación
**Origen en el RFC:** CA-23
- **WHEN** Kafka está caído durante registro, consulta, cambio de estado y ciclo publicador, y luego vuelve
- **THEN** las APIs de negocio siguen disponibles, los eventos quedan pendientes y se publican en orden al recuperarse; readiness no depende de Kafka

#### Scenario: Fallo intermedio y concurrencia entre réplicas
**Origen en el RFC:** CA-24, CA-25
- **WHEN** falla el envío de un evento intermedio o dos réplicas ejecutan un ciclo simultáneamente
- **THEN** el ciclo conserva el orden sin avanzar eventos posteriores al fallido, conserva marcados los eventos anteriores confirmados por Kafka, actualiza el gauge con la cantidad pendiente al final del ciclo y el candado evita duplicación concurrente

#### Scenario: Ciclos de una instancia sin solapamiento y errores recuperables
**Origen en el RFC:** F-04, CA-23, CA-24
- **WHEN** un ciclo sigue ejecutándose al llegar el siguiente disparo, o Kafka/PostgreSQL falla durante la publicación
- **THEN** el siguiente ciclo se omite; el job no cae por la excepción, Kafka produce WARN con `idEvento` y PostgreSQL produce ERROR, dejando el trabajo para reintento en el siguiente ciclo

#### Scenario: Métrica de pendientes
**Origen en el RFC:** CA-26
- **WHEN** se consulta la métrica antes y después de publicar eventos
- **THEN** `triaje_outbox_pendientes` refleja la cantidad real pendiente
