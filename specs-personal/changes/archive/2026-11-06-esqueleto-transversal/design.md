# Diseño — esqueleto-transversal

## Endpoints / puntos de entrada
| Flujo RFC | Método y ruta | Request | Response | Errores |
|---|---|---|---|---|
| F-02, F-04 | GET `/actuator/health/liveness` | Sin body; sin API key | Estado liveness | Los indicados por Actuator en el RFC |
| F-02, F-04 | GET `/actuator/health/readiness` | Sin body; sin API key | Estado readiness basado en R2DBC | DOWN/503 al no responder la base |
| F-02, F-04 | GET `/actuator/info` | `X-API-Key` | Info | 401 ProblemDetail |
| F-02, F-04 | GET `/actuator/prometheus` | `X-API-Key` | Métricas | 401 ProblemDetail |
| F-05 | WebFilter para toda respuesta HTTP | Petición HTTP | Cabecera `X-Trace-Id` | Aplica también a errores y 401 |
| F-01 | Arranque de aplicación | Variables DB | Flyway aplica V1 y R2DBC conecta | Arranque falla si migración/base falla |

## Puntos de entrada para el índice
| Método y ruta (o job / evento) | Qué hace | Spec | Cambio |
|---|---|---|---|
| GET `/actuator/health/liveness` | Probe de vida, sin clave | observabilidad.md | MODIFICADO |
| GET `/actuator/health/readiness` | Probe con verificación R2DBC | observabilidad.md | MODIFICADO |
| GET `/actuator/info` | Expone información protegida por API key | seguridad.md | MODIFICADO |
| GET `/actuator/prometheus` | Expone métricas protegidas por API key | seguridad.md | MODIFICADO |
| Manejador global de errores | Traduce errores a ProblemDetail | errores.md | NUEVO |
| Arranque / Flyway | Aplica el esquema inicial | esquema-persistencia.md | NUEVO |
| WebFilter HTTP | Añade `X-Trace-Id` | observabilidad.md | NUEVO |
| Docker Compose | Inicia dependencias y Triage local | entorno-calidad.md | NUEVO |
| `check` Gradle | Aplica arquitectura, BlockHound y umbral JaCoCo | entorno-calidad.md | NUEVO |

## Paradigma y stack
Reactivo: Java 21, Spring Boot 3.5.16, WebFlux y R2DBC con PostgreSQL 16. Flyway usa JDBC solo durante el arranque, fuera del event loop. Gradle 9.2.1. Estructura hexagonal + hexagonal con CQRS en módulos `dominio`, `aplicacion` e `infraestructura`, con paquetes y vocabulario en español. La estructura base ya existe.

## Capas
| Capa | Clases nuevas o tocadas |
|---|---|
| dominio | Excepciones comunes de negocio en `co.vidacruz.triage.comun.excepcion` |
| aplicacion | Ninguna lógica de negocio ni endpoints en esta entrega |
| driven-adapters | No aplica al estructura hexagonal con CQRS del proyecto; adaptadores R2DBC/health existentes solo si el RFC requiere integración |
| entry-points | Configuración de seguridad WebFlux, manejador global, WebFilter de trace, configuración Actuator |
| infraestructura (configuración/recursos) | Flyway V1, configuración de propiedades, Dockerfile, compose, `.env.example`, README, build y pruebas |

## Decisiones
- Los flujos F-01 (variables de conexión), F-04 (exposición actual del actuator) y F-07/RN-07/CA-25 (ArquitecturaTest) se marcan MODIFICADO en el RFC porque cambian código de la estructura inicial. Como esta es la primera entrega con specs y no hay línea base, los requisitos correspondientes se documentan como NUEVOS en este delta; no se reconstruye una línea base desde el código. Nota: decisión del copiloto, 2026-11-06.
- Flyway usa JDBC al arranque y el tráfico de aplicación usa R2DBC, según sección 3 del RFC.
- Kafka y Redis corren solo en Compose; el Triage no los integra en código ni readiness en esta entrega.
- Solo health queda público; info y prometheus requieren API key.
- El esquema usa índices y CHECK definidos por el RFC; la auditoría se protege en la base con triggers.
- La puerta JaCoCo agrega los tres módulos y exige 80 % de líneas; exclusiones solo `Aplicacion` y `package-info`.

## Detalles sin definir (sección 8 del RFC)
| Pendiente | Cómo se deja en el código |
|---|---|
| Ninguno | No aplica |

## Tratamiento de la estructura inicial
Los cambios F-01 (variables de conexión), F-04 (exposición de actuator) y F-07/RN-07/CA-25 (`ArquitecturaTest`) modifican código ya presente. Como no hay specs vigentes, se expresan como requirements NUEVOS en este primer delta, conforme a la decisión del copiloto del 2026-11-06. No se reconstruye baseline a partir del código.
