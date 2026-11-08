# entorno-calidad

### Requirement: Entorno local reproducible
El Dockerfile multietapa compila con JDK 21 y ejecuta con JRE 21 bajo usuario no root. Docker Compose levanta PostgreSQL 16, Kafka 4.x, Redis 7 y Triage; el servicio espera health de PostgreSQL, usa `.env` y publica Triage en 8081. `.env.example` contiene solo nombres y valores ilustrativos.

**Origen en el RFC:** F-06, CA-28

#### Scenario: Compose levanta el Triage
**Origen en el RFC:** CA-28
- **WHEN** se configura `.env` a partir de `.env.example` y se ejecuta `docker compose up`
- **THEN** Triage inicia después de PostgreSQL saludable y queda accesible en `localhost:8081`

### Requirement: Calidad automatizada del proyecto
JaCoCo agrega la cobertura de los tres módulos y `check` exige al menos 80 % de líneas; BlockHound se instala en pruebas de integración; ArchUnit verifica clases reales y las dependencias entre capas sin permitir Spring en dominio ni aplicación.

**Origen en el RFC:** F-07, RN-07, CA-25, CA-26, CA-27

#### Scenario: ArchUnit valida clases reales
**Origen en el RFC:** CA-25
- **WHEN** se ejecutan las reglas de arquitectura
- **THEN** importan clases de los módulos por su ubicación y verifican las dependencias reales

#### Scenario: Cobertura menor al umbral falla
**Origen en el RFC:** CA-26
- **WHEN** la cobertura de líneas agregada es inferior a 80 %
- **THEN** `check` falla

#### Scenario: Bloqueo del event loop detectado
**Origen en el RFC:** CA-27
- **WHEN** una prueba de integración ejecuta una operación bloqueante en el event loop
- **THEN** BlockHound reporta la infracción


Las reglas ArchUnit importan clases productivas desde `dominio/build`, `aplicacion/build` e `infraestructura/build`. Verifican dependencias de dominio hacia ninguna otra capa ni Spring; aplicación hacia infraestructura ni Spring; controladores hacia adaptadores; y consultas hacia servicios o entidades. Solo las reglas de controladores y consultas permiten conjunto vacío.
