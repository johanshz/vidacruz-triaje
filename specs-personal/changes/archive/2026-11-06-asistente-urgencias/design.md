# Diseño — asistente-urgencias

## Endpoints / puntos de entrada
| Flujo RFC | Método y ruta | Request | Response | Errores |
|---|---|---|---|---|
| F-09 | GET `/asistente/estado-urgencias` | Sin parámetros ni cuerpo; `X-API-Key` | 200 `DtoEstadoUrgencias` JSON | 401 `NO_AUTORIZADO`, 503 `URGENCIAS_NO_DISPONIBLE`, 500 `ERROR_INTERNO` |
| F-04 | GET `/actuator/health/**` | Sin API key | Health/probes; detalles ocultos | 200 UP en liveness y readiness, también con Redis o Kafka caídos (CA-16, CA-21) |
| F-04 | GET `/actuator/info` | `X-API-Key` | Info actuator | 401 `NO_AUTORIZADO` |
| F-04 | GET `/actuator/prometheus` | `X-API-Key` | Métricas Prometheus | 401 `NO_AUTORIZADO` |

## Puntos de entrada para el índice
| Método y ruta (o job / evento) | Qué hace | Spec | Cambio |
|---|---|---|---|
| Kafka `triaje.eventos`, consumidor `ConsumidorEventosTriaje` | Actualiza proyección de urgencias en Redis | vista-urgencias.md | NUEVO |
| GET `/asistente/estado-urgencias` | Consulta conteos por nivel | vista-urgencias.md | NUEVO |
| GET `/actuator/health/**` | Probes abiertas | plataforma-asistente.md | MODIFICADO |
| GET `/actuator/info` | Información protegida | plataforma-asistente.md | NUEVO |
| GET `/actuator/prometheus` | Métricas protegidas | plataforma-asistente.md | MODIFICADO |

## Paradigma y stack
Java 21, Spring Boot 3.5.16, Gradle 9.2.1. Paradigma reactivo con Spring WebFlux y Spring Data Redis reactivo; Kafka con Spring Kafka. Sin base relacional. Estructura hexagonal + CQRS de Ceiba, en español, en módulos `dominio`, `aplicacion` e `infraestructura`. Dominio depende solo de Reactor; aplicación depende de dominio; infraestructura depende de aplicación y dominio. No aplicar el scaffold Bancolombia.

## Capas
| Capa | Clases nuevas o tocadas |
|---|---|
| dominio | `co.vidacruz.asistente.comun.excepcion`; `co.vidacruz.asistente.urgencias.modelo.entidad`, `co.vidacruz.asistente.urgencias.modelo.dto`, `co.vidacruz.asistente.urgencias.servicio`, `co.vidacruz.asistente.urgencias.puerto.repositorio`, `co.vidacruz.asistente.urgencias.puerto.dao`, `co.vidacruz.asistente.urgencias.excepcion`. Clases: excepciones de negocio, `ServicioActualizarUrgencias`, `RepositorioUrgencias`, `DaoUrgencias`, DTO de estado/evento. |
| aplicacion | `co.vidacruz.asistente.urgencias.comando` (`ComandoActualizarUrgencias`, `ManejadorActualizarUrgencias`) y `co.vidacruz.asistente.urgencias.consulta` (`ManejadorConsultarEstadoUrgencias`). |
| infraestructura | `co.vidacruz.asistente.urgencias.controlador` (`ConsultaControladorUrgencias`); `co.vidacruz.asistente.urgencias.consumidor` (`ConsumidorEventosTriaje`); `co.vidacruz.asistente.urgencias.adaptador.repositorio` (`RepositorioUrgenciasRedis`); `co.vidacruz.asistente.urgencias.adaptador.dao` (`DaoUrgenciasRedis`); `co.vidacruz.asistente.configuracion` (Kafka, Redis, seguridad, tracing y actuator); `ManejadorGlobalErrores` y filtro `X-Trace-Id`. |

## Decisiones
- Mantener la vista en cinco conjuntos Redis sin TTL ni registro de eventos procesados — el conjunto por paciente aporta idempotencia y la llave Kafka conserva el orden por partición.
- Validar en `ServicioActualizarUrgencias` los campos requeridos del evento y el nivel de `paciente_registrado`; el consumidor filtra tipo/versión no soportados y arma el comando para eventos procesables.
- Procesar eventos mediante listener que devuelve `Mono` — el offset se confirma al completar la operación reactiva.
- Mantener la consulta F-09 sin servicio ni entidad de dominio: controlador → manejador de consulta → puerto `DaoUrgencias` → `DaoUrgenciasRedis`, que produce `DtoEstadoUrgencias` con niveles ordenados, total y hora de consulta.
- Readiness no incluye Redis ni Kafka — el GET debe poder responder 503 cuando Redis está caído.
- Declarar solo `triaje.eventos.DLT` desde el Asistente — el topic de entrada pertenece al Triage.
- Usar Testcontainers con Kafka y Redis reales y BlockHound — así se verifica el contrato de integración y la ausencia de bloqueo.
- Tiempo máximo de comando Redis P-01 queda configurable; el RFC no fija valor.

## Detalles sin definir (sección 8 del RFC)
| Pendiente | Cómo se deja en el código |
|---|---|
| P-01: valor exacto de `spring.data.redis.timeout` | Configurable en propiedades y documentado en README; elegir un valor que permita responder CA-14 en pocos segundos, sin fijarlo en el plan. |

## Clasificación de CA-28
El RFC marca CA-28 como `MODIFICADO` porque cambia `infraestructura/src/test/java/co/vidacruz/asistente/ArquitecturaTest.java`, que ya existe en el código. Como no hay specs vigentes ni requirement anterior, `plataforma-asistente.md` registra la regla como NUEVA. El RFC permanece intacto.
