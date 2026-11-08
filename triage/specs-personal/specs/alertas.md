# alertas

### Requirement: Consumo replicado de escalaciones y emisión de alertas
Cada réplica consume `triaje.eventos` en grupo `triage-alertas-<instancia>` con `auto.offset.reset=latest`. Solo procesa `paciente_escalado`; usa PostgreSQL para nombre y fecha de registro, calcula minutos enteros truncados, y emite alerta con `idPaciente`, `nombre`, `nivel`, `minutosEsperando`, `escaladoEn`. Descarta duplicados con memoria acotada y pacientes inexistentes con WARN. Mensajes mal formados reintentan tres veces con espera creciente y van a DLT. La indisponibilidad de Kafka no impide servir APIs.

**Origen en el RFC:** RN-10, RN-11, RN-13

#### Scenario: Evento escalado se convierte en alerta
**Origen en el RFC:** CA-13
- **WHEN** llega un evento válido de paciente existente
- **THEN** publica al sink la alerta con los campos contratados, `escaladoEn` igual a `ocurridoEn`, y registra `ALERTA_ESCALAMIENTO`

#### Scenario: Evento inexistente o duplicado se descarta
**Origen en el RFC:** CA-16
- **WHEN** el evento refiere paciente inexistente o `idEvento` ya procesado
- **THEN** no emite alerta, registra WARN y no envía a DLT

#### Scenario: Mensaje inválido va a DLT
**Origen en el RFC:** CA-17
- **WHEN** el JSON es inválido o falta `idPaciente`
- **THEN** realiza tres reintentos con espera creciente, lo publica en `triaje.eventos.DLT` y continúa con otros mensajes

#### Scenario: Otros eventos se ignoran
**Origen en el RFC:** CA-18
- **WHEN** llega un tipo distinto de `paciente_escalado`
- **THEN** no emite alerta ni registra error

#### Scenario: Réplicas reciben todas las alertas
**Origen en el RFC:** CA-19
- **WHEN** dos réplicas con grupos propios reciben un evento escalado
- **THEN** cada réplica emite la alerta a sus suscriptores

### Requirement: Flujo SSE de estado inicial y alertas nuevas
`GET /triage/alertas/flujo` exige `X-API-Key`, responde como `text/event-stream`, emite primero escalados no ALTA (incluidos EN_CONSULTA) con fecha de auditoría y luego alertas nuevas. No acepta clave por query string. Al desconectarse el cliente se cancela su suscripción. Los suscriptores lentos se descartan o cortan sin frenar Kafka ni los demás suscriptores.

**Origen en el RFC:** RN-12, RN-14, RN-18

#### Scenario: Estado inicial precede nuevas alertas
**Origen en el RFC:** CA-14
- **WHEN** se conecta un cliente al flujo
- **THEN** recibe primero pacientes escalados no ALTA, después nuevos eventos; excluye pacientes no escalados y ALTA

#### Scenario: Seguridad del endpoint SSE
**Origen en el RFC:** CA-15
- **WHEN** la clave falta o es inválida
- **THEN** responde 401 `NO_AUTORIZADO` y no abre el flujo

#### Scenario: Suscriptor lento no bloquea difusión
**Origen en el RFC:** CA-20
- **WHEN** un suscriptor deja de consumir más allá de la capacidad del buffer
- **THEN** se descarta o corta ese suscriptor, mientras Kafka y suscriptores sanos continúan
