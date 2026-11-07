# esquema-persistencia

### Requirement: Esquema inicial y migraciones al arrancar
Flyway debe crear `paciente`, `auditoria` y `outbox` en PostgreSQL 16 al iniciar; R2DBC debe poder consultar las tres tablas. La configuración JDBC y R2DBC se compone de `DB_HOST`, `DB_PORT` (por defecto 5432) y `DB_NAME`, junto con `DB_USER` y `DB_PASSWORD`; `DB_URL` deja de usarse. La auditoría admite INSERT y rechaza UPDATE, DELETE y TRUNCATE. La base valida los catálogos descritos en el RFC y el outbox asigna una secuencia creciente a sus filas pendientes. La aplicación no arranca si Flyway no puede conectarse o aplicar el esquema.

**Origen en el RFC:** F-01, RN-01, RN-06, RN-08, CA-01, CA-02, CA-03, CA-04, CA-05, CA-06, CA-07, CA-08

#### Scenario: Flyway crea las tablas y R2DBC las consulta
**Origen en el RFC:** CA-01
- **WHEN** arranca el Triage contra PostgreSQL 16 vacío
- **THEN** existen las tres tablas con columnas, índices y disparadores especificados y las tres admiten consulta R2DBC

#### Scenario: Auditoría permite insertar una fila
**Origen en el RFC:** CA-02
- **WHEN** se inserta una auditoría para un paciente existente
- **THEN** la fila queda guardada

#### Scenario: Auditoría rechaza UPDATE, DELETE y TRUNCATE
**Origen en el RFC:** CA-03, CA-04, CA-05
- **WHEN** se intenta cualquiera de esas operaciones sobre `auditoria`
- **THEN** la base lanza `auditoria es inmutable` y conserva la fila

#### Scenario: Los catálogos rechazan valores desconocidos
**Origen en el RFC:** CA-06
- **WHEN** se intenta insertar un nivel, estado o tipo fuera del catálogo
- **THEN** el CHECK correspondiente rechaza el insert

#### Scenario: Outbox asigna secuencia creciente
**Origen en el RFC:** CA-07
- **WHEN** se insertan dos eventos seguidos
- **THEN** el segundo tiene mayor `secuencia` y ambos quedan pendientes

#### Scenario: Fallo de base durante arranque
**Origen en el RFC:** CA-08
- **WHEN** PostgreSQL no existe o no responde durante el arranque
- **THEN** Flyway falla y el servicio no queda arriba

