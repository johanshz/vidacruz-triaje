# Diseño — camino-principal-paciente

## Endpoints / puntos de entrada
| Flujo RFC | Método y ruta | Request | Response | Errores |
|---|---|---|---|---|
| F-01 | POST `/triage/pacientes` | JSON con nombre, edad, signos vitales y queja; `X-API-Key` | 201: `id`, `nivel`, `slaMinutos`, `estado`, `fechaRegistro` | 400 `VALIDACION`, 401 `NO_AUTORIZADO`, 500 `ERROR_INTERNO` |
| F-02 | GET `/triage/pacientes/{id}` | UUID; `X-API-Key` | 200: ficha completa, estado, `escalado`, fechas y `tiempoEnEstadoActualMinutos` | 400 `VALIDACION`, 401 `NO_AUTORIZADO`, 404 `NO_ENCONTRADO` |
| F-03 | PATCH `/triage/pacientes/{id}/estado` | JSON `{ "estado": ... }`; `X-API-Key` | 200: ficha completa | 400 `VALIDACION`, 401 `NO_AUTORIZADO`, 404 `NO_ENCONTRADO`, 409 `TRANSICION_INVALIDA`, 500 `ERROR_INTERNO` |
| F-04 | Job periódico del publicador outbox | Configuración `vidacruz.outbox.intervalo` y `vidacruz.outbox.lote` | Publica JSON en `triaje.eventos`, llave `idPaciente` | Si Kafka falla, conserva pendientes y registra WARN; el ciclo siguiente reintenta |

## Puntos de entrada para el índice
| Método y ruta (o job / evento) | Qué hace | Spec | Cambio |
|---|---|---|---|
| POST `/triage/pacientes` | Registra, clasifica y persiste ficha, auditoría y outbox | paciente.md | NUEVO |
| GET `/triage/pacientes/{id}` | Consulta ficha mediante DAO/MapRow | paciente.md | NUEVO |
| PATCH `/triage/pacientes/{id}/estado` | Cambia estado con auditoría y outbox | estado-paciente.md | NUEVO |
| Job periódico del outbox | Publica eventos pendientes en Kafka | publicacion-eventos.md | NUEVO |

## Paradigma y stack
Java 21, Spring Boot 3.5.16, Gradle 9.2.1. Paradigma reactivo: Spring WebFlux y R2DBC, PostgreSQL 16; Kafka con Spring for Apache Kafka (`KafkaTemplate` mediante `Mono.fromFuture`, `KafkaAdmin`). Arquitectura hexagonal + hexagonal con CQRS, módulos `dominio`, `aplicacion`, `infraestructura`, código y vocabulario en español. No agregar migración: el esquema V1 existente cubre paciente, auditoría y outbox.

## Capas
| Capa | Clases nuevas o tocadas |
|---|---|
| dominio | `paciente`: entidades, DTO de lectura, servicio de clasificación/validación y transiciones, puertos de repositorio/DAO y transacción; `evento`: modelo, puerto y reglas del outbox; excepciones de negocio |
| aplicacion | Comandos, manejadores, fábricas y manejadores de consulta para registrar, consultar y cambiar estado; orquestación de publicación |
| infraestructura | Controladores WebFlux, `DaoPacientePostgres` y MapRow, adaptadores R2DBC de paciente/auditoría/outbox, adaptador de transacción, productor Kafka, configuración de topic/métricas y tarea periódica |
| entry-points | Endpoints HTTP de F-01 a F-03 y tarea programada F-04 dentro del módulo `infraestructura` según el layout real del proyecto |

## Decisiones
- Mantener los módulos Gradle y paquetes de la estructura hexagonal con CQRS existente, porque el RFC define su propia estructura.
- F-01 y F-03 escriben ficha, auditoría y outbox en una misma transacción. El límite de transacción se implementa con un puerto de dominio y adaptador `TransactionalOperator`, recomendado por el RFC para no acoplar aplicación a Spring.
- F-02 consulta por `DaoPaciente` + `DatabaseClient` + MapRow, sin entidad ni servicio, por hexagonal con CQRS.
- Inyectar `java.time.Clock` UTC para tiempos reproducibles.
- El publicador usa candado consultivo PostgreSQL por ciclo y procesa por `secuencia`; `enviado_en` se actualiza tras la confirmación Kafka.
- El productor Kafka configura `StringSerializer` para llave y valor, `acks=all` e idempotencia activada; publica como valor el JSON de `outbox.carga` sin transformarlo.
- Los ciclos del job en una misma instancia no se solapan; si el ciclo anterior sigue ejecutándose, se omite el siguiente.
- El job captura las excepciones para no caer: ante fallo Kafka registra WARN con `idEvento`; ante fallo PostgreSQL registra ERROR y reintenta en el ciclo siguiente.
- Crear topic `triaje.eventos` con tres particiones cuando Kafka esté disponible. No agregar dependencia de Redis ni llamada al Asistente.
- `KAFKA_BOOTSTRAP_SERVERS` es obligatoria; intervalo por defecto `PT1S` y lote por defecto `100`. Tiempo de espera y reintentos internos del productor se definen en propiedades con valores razonables, como permite P-03.
- No se agregan códigos HTTP distintos de los que declara el RFC.

## Detalles sin definir (sección 8 del RFC)
| Pendiente | Cómo se deja en el código |
|---|---|
| P-01: nombres JSON exactos propuestos en camelCase español | Usar los nombres del contrato de F-01/F-02 de esta RFC, coherentes también en F-03; sin aliases adicionales. |
| P-02: códigos y cuerpo de F-01/F-03 propuestos | Aplicar 201 para F-01 y 200 con ficha completa para F-03 tal como fija el contrato de la RFC. |
| P-03: lote, timeout y reintentos Kafka | Usar lote 100; hacer configurables timeout y reintentos internos en propiedades, sin fijar valores de negocio. |
| P-04: Kafka caído al arranque y autocreación con una partición | La RFC no define mecanismo de reparación tardía; no añadir comportamiento no especificado. La creación con tres particiones aplica si Kafka está disponible al arranque. |
| P-05: ubicación/nombre del puerto transaccional | Crear `EjecutorTransaccion` en dominio y adaptarlo con `TransactionalOperator` en infraestructura, tal como recomienda el RFC. |
| P-06: recorte y medición de nombre/queja | Seguir el contrato de F-01: medir tras `trim` y guardar recortados. |
