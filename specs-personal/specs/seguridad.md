# seguridad

### Requirement: ProtecciÃ³n de rutas con API key
La configuraciÃ³n `vidacruz.seguridad.api-key` debe validarse al arrancar y comparar la cabecera `X-API-Key` en tiempo constante. Solo `/actuator/health/**` queda abierto. Las rutas protegidas rechazan claves ausentes o erradas con 401 ProblemDetail y `NO_AUTORIZADO`.

**Origen en el RFC:** RN-02, RN-03, CA-09, CA-10, CA-11, CA-12, CA-13

#### Scenario: Clave ausente o errada
**Origen en el RFC:** CA-09, CA-10
- **WHEN** se consulta info o prometheus sin clave o con una clave incorrecta
- **THEN** responde 401 `application/problem+json` con `NO_AUTORIZADO`, detalle establecido y `traceId`

#### Scenario: Clave vÃ¡lida
**Origen en el RFC:** CA-11
- **WHEN** se consulta `/actuator/info` o `/actuator/prometheus` con la clave correcta
- **THEN** responde 200

#### Scenario: Health no requiere clave
**Origen en el RFC:** CA-12
- **WHEN** se consultan liveness y readiness sin clave
- **THEN** ambos responden 200 y estado `UP` cuando la base estÃ¡ disponible

#### Scenario: Clave requerida al iniciar
**Origen en el RFC:** CA-13
- **WHEN** la propiedad estÃ¡ ausente o vacÃ­a
- **THEN** la validaciÃ³n impide iniciar el contexto

