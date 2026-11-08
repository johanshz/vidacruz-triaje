# paciente

## MODIFICADO

### Requirement: Registro y clasificación transaccional
El endpoint registra pacientes válidos, aplica clasificación de triaje y SLA, y persiste ficha, auditoría y evento outbox en una única transacción. La clasificación evalúa gravedad de nivel 1 a 5; la validación de tipos y rangos se realiza en dominio. El SLA por nivel procede de la configuración inyectada sin dependencias Spring, con defaults 5, 10, 30, 60 y 120 minutos; las propiedades `vidacruz.sla.minutos.{critico,urgente,urgente-2,no-urgente,no-urgente-2}` (variables `VIDACRUZ_SLA_MINUTOS_*`) deben ser enteros mayores que 0; un valor inválido impide arrancar. `VIDACRUZ_REPORTE_ZONA_HORARIA` debe ser un ID de zona válido; un valor inválido también impide arrancar. Cambiar la configuración afecta solo a pacientes nuevos; cada ficha conserva el SLA con que se registró.

**Origen en el RFC:** F-01, RN-01

#### Scenario: SLA configurado para nuevas fichas
**Origen en el RFC:** CA-01
- **WHEN** se registra un paciente CRITICO con la propiedad configurada en 1 minuto
- **THEN** responde 201 y tanto la respuesta como la ficha guardada contienen `slaMinutos = 1`; los defaults conservan los SLA de los ejemplos E1 a E6

#### Scenario: Propiedades inválidas impiden iniciar
**Origen en el RFC:** CA-03
- **WHEN** cualquier `VIDACRUZ_SLA_MINUTOS_*` contiene un valor no entero o menor o igual a cero, o `VIDACRUZ_REPORTE_ZONA_HORARIA` contiene un ID de zona inválido
- **THEN** el contexto no arranca y el error identifica la propiedad inválida

#### Scenario: Contrato previo del registro se conserva
**Origen en el RFC:** CA-01
- **WHEN** se registra un paciente válido bajo configuración válida
- **THEN** request, respuesta 201, clasificación, auditoría, outbox y errores mantienen el contrato vigente, excepto el valor SLA configurable
