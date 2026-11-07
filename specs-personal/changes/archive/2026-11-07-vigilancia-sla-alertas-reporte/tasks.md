# Tareas — vigilancia-sla-alertas-reporte

## 1. Configuración y dominio
- [x] 1.1 Crear propiedades validadas de SLA, intervalo, instancia y zona horaria; valores por defecto; validar las cinco `VIDACRUZ_SLA_MINUTOS_*` como enteros mayores que 0 y `VIDACRUZ_REPORTE_ZONA_HORARIA` como ID de zona válido — sirve a: F-01, F-02, F-03, F-05, RN-01, RN-10, RN-15
- [x] 1.2 Reemplazar constantes de SLA por objeto de configuración de dominio sin Spring — sirve a: F-01, RN-01, CA-01, CA-03
- [x] 1.3 Crear dominio y puertos para escalar pacientes vencidos; preservar consulta y estado del flag — sirve a: F-02, RN-02, RN-03, RN-04, RN-06
- [x] 1.4 Crear comando y manejador de escalamiento con reloj inyectado — sirve a: F-02, RN-02, RN-08
- [x] 1.5 Crear DTO/puerto de consulta y emisión de alerta y comando/manejador de emisión — sirve a: F-03, RN-10, RN-11, RN-13
- [x] 1.6 Crear validación de rango y DTOs de reporte horario — sirve a: F-05, RN-15, RN-17
- [x] 1.7 Crear manejador de consulta de reporte sin servicio ni entidad — sirve a: F-05, RN-15, RN-17

## 2. Adaptadores y entry points
- [x] 2.1 Implementar UPDATE condicional con RETURNING y registrar auditoría/outbox en transacción existente — sirve a: F-02, RN-03, RN-05, RN-07
- [x] 2.2 Implementar tarea programada no solapable, captura de errores y métrica post-commit — sirve a: F-02, RN-08, RN-09
- [x] 2.3 Implementar consumidor Kafka por réplica, filtrado de tipos, deduplicación, reintentos y DLT — sirve a: F-03, RN-10, RN-13
- [x] 2.4 Implementar DAO de alerta para nombre, registro y escalados activos con fecha de auditoría — sirve a: F-03, F-04, RN-11, RN-12
- [x] 2.5 Implementar sink multicast con buffer y política de suscriptor lento configurable — sirve a: F-03, F-04, RN-14
- [x] 2.6 Implementar controlador SSE autenticado con estado inicial antes de alertas nuevas — sirve a: F-04, RN-12, RN-18
- [x] 2.7 Implementar DAO PostgreSQL de reporte en una sentencia con agregados, p95 y ventana; MapRow; `$documentar-api` declara `horas[].acumuladoRegistrados` como adición propia en `docs/api/openapi.yaml` — sirve a: F-05, RN-15, RN-16
- [x] 2.8 Implementar controlador de reporte, validación de parámetros, ProblemDetail y API key — sirve a: F-05, RN-17, RN-18
- [x] 2.9 Actualizar `.env.example` y `docker-compose.yml` con nombres de variables nuevas — sirve a: F-01, F-02, F-03, F-05
- [x] 2.10 Documentar en README configuración y pruebas de SLA, SSE y reporte, incluida limitación EventSource — sirve a: CA-30

## 3. Tests unitarios
- [x] 3.1 Ajustar tests de registro para SLA configurable/defaults y propiedades inválidas — sirve a: CA-01, CA-03
- [x] 3.2 Probar selección por borde del SLA, idempotencia, estados y flag persistente — sirve a: CA-02, CA-04, CA-05, CA-06, CA-08, CA-09
- [x] 3.3 Probar rollback, sobre del evento, métrica y ciclos no solapables — sirve a: CA-10, CA-11, CA-12
- [x] 3.4 Probar consumidor: tipo ignorado, payload, duplicado, paciente inexistente, reintentos/DLT — sirve a: CA-13, CA-16, CA-17, CA-18
- [x] 3.5 Probar armado de reporte y validaciones ISO/rango/campos — sirve a: CA-21, CA-22, CA-23, CA-24, CA-25, CA-26, CA-27

## 4. Tests de integración y calidad
- [x] 4.1 Probar escalamiento concurrente contra PostgreSQL real y atomicidad UPDATE/auditoría/outbox — sirve a: CA-07, CA-10
- [x] 4.2 Probar publicación Kafka, DLT y difusión a dos réplicas con Testcontainers — sirve a: CA-11, CA-13, CA-17, CA-19
- [x] 4.3 Probar SSE inicial, flujo nuevo, seguridad, desconexión y lentitud — sirve a: CA-14, CA-15, CA-20
- [x] 4.4 Probar query de reporte PostgreSQL: zona, bordes, filtros, null, p95 y acumulado — sirve a: CA-21, CA-22, CA-23, CA-24, CA-25, CA-26
- [x] 4.5 Probar endpoints de reporte con autenticación y errores de parámetros — sirve a: CA-27, CA-28
- [x] 4.6 Mantener BlockHound, ArquitecturaTest y cobertura mínima 80 % para el alcance nuevo — sirve a: CA-29

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** 28 de 28 tareas marcadas. `clean test` y `build` terminaron en `BUILD SUCCESSFUL`; JaCoCo agregado: 96,73 % de líneas y 84,92 % de ramas, con BlockHound y `ArquitecturaTest` en verde.
- **Faltó:** Nada.
- **Docs vivos tocados:** `README.md`, `docs/api/openapi.yaml`, `.env.example`, `docker-compose.yml`, `../contexto/tecnico.md` y este `tasks.md`.
- **Sigue:** No quedan tareas de construcción. Commit pendiente de Johan, según la regla 5 del `AGENTS.md` raíz.
- **Docs vivos a actualizar:** Ninguno; `ESTADO.md` queda para el orquestador.