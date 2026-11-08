# vigilancia

## NUEVO

### Requirement: Escalamiento transaccional e idempotente de pacientes vencidos
El job programado usa `Clock` y el SLA guardado en la ficha. Solo marca una vez a pacientes REGISTRADO no escalados cuando `ahora >= fecha_registro + sla_minutos`. La actualización condicional, auditoría `paciente_escalado` y outbox se confirman en una transacción. El estado no cambia y el flag persiste tras consulta o alta. Los ciclos no se solapan; capturan errores, registran ERROR y siguen en el siguiente ciclo. Tras commit incrementa `triaje.escalamientos` una vez por paciente. El evento conserva el sobre, llave y fechas definidos por el RFC.

**Origen en el RFC:** RN-02, RN-03, RN-04, RN-05, RN-06, RN-07, RN-08, RN-09

#### Scenario: El job respeta el SLA guardado al registrar
**Origen en el RFC:** CA-02
- **WHEN** cambia la configuración global después de registrar un paciente
- **THEN** la vigilancia compara contra el `sla_minutos` persistido en esa ficha

#### Scenario: Se escala en el vencimiento exacto
**Origen en el RFC:** CA-04, CA-05
- **WHEN** corre el job para pacientes registrados no escalados en el borde del SLA o un segundo antes
- **THEN** escala solo al que llegó al instante exacto, conserva REGISTRADO y escribe auditoría y outbox con el sobre de RN-07

#### Scenario: No escala dos veces ni pacientes fuera de espera
**Origen en el RFC:** CA-06, CA-08
- **WHEN** el job vuelve a ejecutarse para un paciente ya escalado o vencido en EN_CONSULTA/ALTA
- **THEN** no duplica auditoría/evento ni escala pacientes en esos estados

#### Scenario: Una sola ejecución gana entre réplicas
**Origen en el RFC:** CA-07
- **WHEN** dos ejecuciones concurrentes actualizan al mismo paciente vencido en PostgreSQL
- **THEN** solo se crea una auditoría y un outbox, y la métrica suma uno

#### Scenario: Fallo transaccional revierte el escalamiento
**Origen en el RFC:** CA-10
- **WHEN** falla la escritura de auditoría u outbox
- **THEN** se revierte el flag, no se incrementa la métrica, queda log ERROR y el job sigue disponible

#### Scenario: El publicador entrega el evento de escalamiento
**Origen en el RFC:** CA-11
- **WHEN** el publicador existente procesa el outbox confirmado
- **THEN** publica en `triaje.eventos` el evento `paciente_escalado`, con llave `idPaciente`, versión 1 y `ocurridoEn` igual a auditoría

#### Scenario: Ciclos sin solapamiento y resiliencia a error
**Origen en el RFC:** CA-12
- **WHEN** un disparo ocurre mientras sigue el ciclo anterior o cuando PostgreSQL falla
- **THEN** omite el solapado y captura/loguea la excepción para permitir ciclos posteriores
