# Tareas — camino-principal-paciente

## 1. Estructura y configuración
- [x] 1.1 Añadir dependencias Spring Kafka y Testcontainers Kafka donde corresponda, conservar módulos existentes — sirve a: F-04, CA-20, CA-22
- [x] 1.2 Configurar `KAFKA_BOOTSTRAP_SERVERS`, intervalo `PT1S`, lote `100`, `.env.example` y servicio Triage del compose con dependencia de salud Kafka — sirve a: F-04, CA-22, CA-23, CA-28
- [x] 1.3 Añadir configuración Kafka y creación del topic de tres particiones con `KafkaAdmin` y `fatalIfBrokerNotAvailable=false`, evitando que la indisponibilidad del broker impida el arranque — sirve a: F-04, RN-12, CA-22, CA-23

## 2. Dominio
- [x] 2.1 Crear modelo de registro y servicio de validación de rangos y clasificación de niveles 1 a 5 — sirve a: F-01, RN-01, RN-02, RN-03, RN-04, CA-01, CA-03 a CA-08
- [x] 2.2 Definir transiciones de estado y reglas de fechas — sirve a: F-03, RN-06, RN-07, RN-08, CA-15 a CA-18
- [x] 2.3 Definir puertos de paciente, auditoría, outbox y ejecución transaccional; DTO de lectura y `DaoPaciente` — sirve a: F-01, F-02, F-03, RN-05, RN-13, CA-02, CA-09, CA-12
- [x] 2.4 Definir modelo/puertos de evento y contrato del sobre versionado (`idEvento`, tipo, versión, `ocurridoEn`, datos de registro/cambio y `estadoAnterior` según el tipo) — sirve a: F-04, CA-21, CA-15

## 3. Casos de uso
- [x] 3.1 Implementar comando, fábrica y manejador de registro transaccional — sirve a: F-01, RN-05, CA-02, CA-09, CA-10
- [x] 3.2 Implementar manejador de consulta directa por DAO — sirve a: F-02, RN-13, CA-12 a CA-14
- [x] 3.3 Implementar comando y manejador transaccional de cambio de estado con arbitraje condicional — sirve a: F-03, RN-05, RN-06 a RN-09, CA-15 a CA-19
- [x] 3.4 Implementar ciclo de publicación secuencial, cancelación ante fallo y contabilización de pendientes; capturar excepciones para que el job no caiga, con WARN + `idEvento` ante fallo Kafka y ERROR/reintento en siguiente ciclo ante fallo PostgreSQL — sirve a: F-04, RN-10 a RN-12, CA-20, CA-23 a CA-26

## 4. Adaptadores
- [x] 4.1 Implementar adaptadores R2DBC de paciente, auditoría y outbox con escritura atómica — sirve a: F-01, F-03, RN-05, CA-02, CA-09, CA-15, CA-19
- [x] 4.2 Implementar adaptador transaccional basado en `TransactionalOperator` — sirve a: F-01, F-03, RN-05, CA-09, CA-19
- [x] 4.3 Implementar DAO SQL y MapRow para consulta sin entidad ni servicio — sirve a: F-02, RN-13, CA-12 a CA-14
- [x] 4.4 Implementar productor Kafka reactivo con `StringSerializer` para llave y valor, `acks=all`, idempotencia activada y valor igual al JSON de `outbox.carga` sin transformar; agregar candado consultivo PostgreSQL y marcado tras confirmación — sirve a: F-04, RN-10, RN-11, CA-20 a CA-25
- [x] 4.5 Implementar métricas de clasificaciones y pendientes outbox; incrementar `triaje.clasificaciones` solo después de confirmar la transacción y no incrementarla para registros rechazados con 400 — sirve a: F-01, F-04, CA-10, CA-26

## 5. Entry points y documentación
- [x] 5.1 Implementar controladores WebFlux de registro, consulta y cambio de estado usando la seguridad y errores vigentes — sirve a: F-01, F-02, F-03, CA-05, CA-11 a CA-18
- [x] 5.2 Implementar tarea programada de publicación según intervalo configurable; impedir solapamiento de ciclos por instancia y omitir el siguiente ciclo mientras el anterior siga ejecutándose — sirve a: F-04, CA-20, CA-23, CA-24
- [x] 5.3 Documentar en README curls E1-E6, GET, PATCH válido e inválido, inspección Kafka y `KAFKA_BOOTSTRAP_SERVERS` — sirve a: CA-28

## 6. Tests unitarios
- [x] 6.1 Probar niveles, SLA, casos E1-E6, prioridades y fronteras — JUnit 5 + StepVerifier — sirve a: CA-01, CA-03
- [x] 6.2 Probar normalización administrativa, validaciones, límites y campos inválidos — JUnit 5 + StepVerifier — sirve a: RN-03, RN-04, CA-04 a CA-08
- [x] 6.3 Probar comandos de registro/transición, atomicidad mediante puertos simulados y fechas con Clock — JUnit 5 + Mockito + StepVerifier — sirve a: RN-05 a RN-09, CA-02, CA-09, CA-15 a CA-19
- [x] 6.4 Probar consulta DAO, errores de identificador y cálculo truncado de minutos — JUnit 5 + Mockito + StepVerifier — sirve a: RN-13, CA-12 a CA-14
- [x] 6.5 Probar contrato productor, orden, fallo intermedio, candado y métricas con dobles unitarios — JUnit 5 + Mockito + StepVerifier — sirve a: RN-10 a RN-12, CA-20 a CA-26

## 7. Pruebas de integración y arquitectura
- [x] 7.1 Probar endpoints y persistencia con WebTestClient y Testcontainers PostgreSQL — sirve a: CA-02, CA-05 a CA-09, CA-11 a CA-19
- [x] 7.2 Probar publicación y contrato con Kafka Testcontainers, incluyendo indisponibilidad y dos ejecuciones — sirve a: CA-20 a CA-26
- [x] 7.3 Ejecutar BlockHound, ArquitecturaTest y cobertura existente con flujos nuevos — sirve a: CA-27

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** `7.3` verificada. `gradlew.bat --offline --no-daemon test` terminó con `BUILD SUCCESSFUL` en 4 min 57 s; `gradlew.bat --offline --no-daemon check` terminó con `BUILD SUCCESSFUL` en 20 s, incluyendo `verificarCoberturaAgregada` y `testCodeCoverageReport`. La suite completa, incluida `PublicacionKafkaIntegracionTest`, pasó.
- **Faltó:** nada.
- **Docs vivos tocados:** este `tasks.md`.
- **Sigue:** D-BUILD queda listo para que el orquestador avance a la siguiente etapa. Commit pendiente de Johan; no hice commit ni push.
- **Reporte JaCoCo:** `codigo/Triage/build/reports/jacoco/test/jacocoTestReport.xml`.
- **Docs vivos a actualizar:** ninguno.
