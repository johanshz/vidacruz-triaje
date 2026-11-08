# plataforma-asistente

### Requirement: Configuración de Kafka y Redis
La aplicación configura Redis reactivo con propiedades externas y un consumidor Kafka del grupo `asistente-urgencias`, `earliest`, tres consumidores y 50 registros por poll configurables. No falla si aún no existe `triaje.eventos`; declara únicamente la DLT de tres particiones y una réplica. Usa deserialización JSON sin cabeceras de tipo y observación del listener.

**Origen en el RFC:** CA-24

#### Scenario: Arrancar antes de que exista el topic de entrada
**Origen en el RFC:** CA-24
- **WHEN** Kafka está disponible y no existe `triaje.eventos`
- **THEN** la aplicación arranca y declara `triaje.eventos.DLT` con tres particiones

### Requirement: Seguridad por API key
Todas las rutas salvo `/actuator/health/**` exigen `X-API-Key` igual a la configuración obligatoria `ASISTENTE_API_KEY`. La comparación usa `MessageDigest.isEqual`; no se habilitan HTTP Basic, form login, logout ni CSRF.

**Origen en el RFC:** RN-09; CA-15, CA-16, CA-17

#### Scenario: Rechazar clave ausente o incorrecta
**Origen en el RFC:** CA-15
- **WHEN** se consulta el endpoint sin clave o con una clave incorrecta
- **THEN** responde 401 ProblemDetail con `NO_AUTORIZADO` y el detalle definido en el RFC

#### Scenario: Permitir probes y proteger actuator restante
**Origen en el RFC:** CA-16
- **WHEN** se consultan health, info y prometheus con y sin clave
- **THEN** health está abierto y info/prometheus requieren clave

#### Scenario: Exigir clave al arrancar
**Origen en el RFC:** CA-17
- **WHEN** `ASISTENTE_API_KEY` está ausente o vacía
- **THEN** la aplicación falla al arrancar por configuración inválida

### Requirement: Errores HTTP uniformes
La infraestructura traduce errores conocidos a ProblemDetail RFC 7807 con `codigo`, `detail` claro y `traceId`; los errores internos no exponen trazas ni detalles internos.

**Origen en el RFC:** CA-18, CA-19, CA-20

#### Scenario: Ruta inexistente
**Origen en el RFC:** CA-18
- **WHEN** se solicita una ruta no existente con clave válida
- **THEN** responde 404 con `NO_ENCONTRADO` y traceId

#### Scenario: Método HTTP no permitido
**Origen en el RFC:** CA-19
- **WHEN** se usa POST sobre el endpoint GET
- **THEN** responde 405 con `SOLICITUD_INVALIDA`

#### Scenario: Error no controlado
**Origen en el RFC:** CA-20
- **WHEN** ocurre una excepción no controlada durante la consulta
- **THEN** responde 500 con `ERROR_INTERNO`, detalle genérico y log ERROR con la excepción, sin traza en el cuerpo

### Requirement: Probes, trazas y logs
Actuator expone `health,info,prometheus`, oculta detalles de health, conserva liveness/readiness independientes de Redis y Kafka. Las respuestas HTTP incluyen `X-Trace-Id`; logs de consola son ECS JSON y el listener propaga la observación de trace.

**Origen en el RFC:** CA-21, CA-22, CA-23

#### Scenario: Readiness sigue disponible con Redis caído
**Origen en el RFC:** CA-21
- **WHEN** Redis está indisponible y se consulta readiness
- **THEN** readiness responde 200 UP y la consulta de urgencias responde 503

#### Scenario: Incluir traceId en cada respuesta
**Origen en el RFC:** CA-22
- **WHEN** se reciben respuestas exitosas, 401 o 404
- **THEN** todas incluyen `X-Trace-Id` y los errores lo igualan con el campo del cuerpo

#### Scenario: Emitir logs ECS correlacionados
**Origen en el RFC:** CA-23
- **WHEN** se atiende una petición
- **THEN** el log de consola es JSON ECS e incluye `traceId`

### Requirement: Entorno local reproducible
Docker Compose inicia Redis 7 con AOF y volumen, Kafka KRaft y el Asistente; solo publica `8082:8080`, espera healthchecks y carga configuración desde `.env`. Para las pruebas locales, un servicio o comando de inicio del Compose crea el topic `triaje.eventos` con 3 particiones (**F-06**); el servicio Asistente no crea ese topic. El Dockerfile es multietapa, ejecuta como usuario no root y no incluye healthcheck propio. El README documenta variables, ejecución, publicación de evento DP3, consulta, probes, errores y supuestos S-10/S-11.

**Origen en el RFC:** F-06, CA-25, CA-26

#### Scenario: Levantar el entorno local
**Origen en el RFC:** CA-25
- **WHEN** se copia `.env.example` a `.env` y se ejecuta Compose
- **THEN** Redis y Kafka quedan sanos, solo se publica el puerto 8082 y readiness responde 200

#### Scenario: Persistir la vista al reiniciar Redis
**Origen en el RFC:** CA-26
- **WHEN** se reinicia Redis después de cargar pacientes
- **THEN** la consulta devuelve los mismos conteos

### Requirement: ArchUnit verifica clases reales
Las reglas de `ArquitecturaTest` importan clases de los módulos Gradle reales, no permiten conjuntos vacíos para las reglas de capas y verifican dependencias y ausencia de Spring en dominio y aplicación. En el RFC, la etiqueta `MODIFICADO` de CA-28 describe el cambio sobre el código existente; como no había requirement previo en specs, este requirement se agrega como NUEVO.

**Origen en el RFC:** RN-10, CA-28

#### Scenario: Verificar clases reales de los módulos
**Origen en el RFC:** CA-28
- **GIVEN** `ArquitecturaTest` tiene las clases importadas por módulo Gradle y las reglas de capas no usan `.allowEmptyShould(true)`
- **WHEN** se corre la prueba
- **THEN** las reglas de dirección de dependencias y de ausencia de Spring en dominio y aplicación revisan clases reales y pasan
