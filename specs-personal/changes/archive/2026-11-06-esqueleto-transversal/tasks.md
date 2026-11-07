# Tareas â€” esqueleto-transversal

## 1. ConfiguraciÃ³n existente
- [x] 1.1 Sustituir `DB_URL` por `DB_HOST`, `DB_PORT` y `DB_NAME` en configuraciÃ³n R2DBC y documentar variables â€” sirve a: F-01, RN-02
- [x] 1.2 Incorporar dependencias Flyway JDBC, Spring Security WebFlux, tracing OTel y dependencias de integraciÃ³n indicadas â€” sirve a: F-01, F-02, F-05

## 2. Dominio
- [x] 2.1 Crear `ExcepcionNegocio`, `ExcepcionValidacion`, `ExcepcionRecursoNoEncontrado` y `ExcepcionConflicto` sin dependencias Spring â€” sirve a: F-03, RN-04

## 3. Adaptadores
- [x] 3.1 Crear migraciÃ³n Flyway V1 con tablas, restricciones, Ã­ndices, triggers de auditorÃ­a y secuencia outbox â€” sirve a: F-01, RN-01, RN-06, RN-08
- [x] 3.2 Configurar Flyway JDBC para ejecutar al arranque antes de que el servicio quede disponible â€” sirve a: F-01, CA-01, CA-08
- [x] 3.3 Configurar propiedades validadas para `TRIAGE_API_KEY` â€” sirve a: F-02, RN-02, CA-13
- [x] 3.4 Implementar autenticaciÃ³n WebFlux con comparaciÃ³n constante y acceso pÃºblico exclusivo de health â€” sirve a: F-02, RN-03
- [x] 3.5 Implementar armado compartido de ProblemDetail y `ManejadorGlobalErrores` para excepciones de dominio, decodificaciÃ³n, rutas inexistentes, otros 4xx y errores 500 â€” sirve a: F-03, RN-04
- [x] 3.6 Configurar Actuator, grupos liveness/readiness, contributor R2DBC y ocultamiento de detalles â€” sirve a: F-04, RN-05
- [x] 3.7 Configurar Micrometer tracing sin exportador, propagaciÃ³n Reactor y formato ECS â€” sirve a: F-05
- [x] 3.8 Implementar WebFilter que agregue `X-Trace-Id` a toda respuesta â€” sirve a: F-05, CA-23
- [x] 3.9 Crear Dockerfile multietapa con runtime no-root â€” sirve a: F-06
- [x] 3.10 Crear Compose con PostgreSQL, Kafka, Redis, healthchecks y Triage dependiente de PostgreSQL saludable â€” sirve a: F-06, CA-28
- [x] 3.11 Crear `.env.example`, agregar `.env` al ignore si aÃºn no lo estÃ¡ y documentar arranque/pruebas/cÃ³digos en README â€” sirve a: F-06

## 4. Entry points
- [x] 4.1 Configurar acceso y respuestas de `/actuator/health/**`, `/actuator/info` y `/actuator/prometheus` segÃºn la polÃ­tica de seguridad â€” sirve a: F-02, F-04

## 5. Tests unitarios y de integraciÃ³n
- [x] 5.1 Verificar migraciÃ³n con PostgreSQL Testcontainers, consulta de las tres tablas, fallo de arranque sin base, conexiÃ³n de Flyway y R2DBC mediante `DB_HOST`, `DB_PORT` y `DB_NAME`, y ausencia de `DB_URL` en `application.yaml` â€” sirve a: F-01, CA-01, CA-08
- [x] 5.2 Verificar INSERT y rechazo de UPDATE, DELETE y TRUNCATE en auditorÃ­a â€” sirve a: CA-02, CA-03, CA-04, CA-05
- [x] 5.3 Verificar CHECK de catÃ¡logos y secuencia creciente del outbox â€” sirve a: CA-06, CA-07
- [x] 5.4 Verificar API key ausente, incorrecta, correcta, `/actuator/info` expuesto con 401 sin clave y 200 con clave, health abierto y ausencia de propiedad â€” sirve a: F-04, CA-09, CA-10, CA-11, CA-12, CA-13
- [x] 5.5 Verificar ProblemDetail para validaciÃ³n, decodificaciÃ³n, ruta ausente, conflicto, 405/415 y excepciÃ³n no controlada â€” sirve a: CA-14, CA-15, CA-16, CA-17, CA-18, CA-19, CA-20
- [x] 5.6 Verificar readiness DOWN al fallar R2DBC y health sin detalles â€” sirve a: CA-21, CA-22
- [x] 5.7 Verificar `X-Trace-Id` en respuestas normales, errores y 401, y campos ECS en logs â€” sirve a: CA-23, CA-24
- [x] 5.8 Corregir ArchUnit para importar clases de mÃ³dulos reales y verificar lÃ­mites sin `allowEmptyShould(true)` en reglas afectadas â€” sirve a: F-07, RN-07, CA-25
- [x] 5.9 Configurar agregaciÃ³n JaCoCo de tres mÃ³dulos, verificaciÃ³n de 80 % en `check` y Ãºnicas exclusiones permitidas â€” sirve a: F-07, CA-26
- [x] 5.10 Instalar BlockHound en integraciÃ³n y configurar JVM args requeridos para Java 21 â€” sirve a: F-07, CA-27
- [x] 5.11 AÃ±adir prueba de humo de Compose que verifique Triage disponible en puerto 8081 â€” sirve a: CA-28

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** tareas 1.1–5.11 completadas. Compose levantó PostgreSQL, Kafka, Redis y Triage; PostgreSQL/Kafka/Redis reportaron `healthy` y `/actuator/health/readiness` respondió HTTP 200 con `UP`. El build offline terminó `BUILD SUCCESSFUL`.
- **Faltó:** nada.
- **Docs vivos tocados:** specs vigentes e índice, RFC histórico, `AGENTS.md`, `contexto/tecnico.md`, resumen de desarrollo e informe de smoke. `ESTADO.md` queda para el orquestador.
- **Sigue:** Johan registra el commit; el orquestador registra el cierre y puede anotar el pendiente no bloqueante de DNS intermitente de Docker Desktop.
