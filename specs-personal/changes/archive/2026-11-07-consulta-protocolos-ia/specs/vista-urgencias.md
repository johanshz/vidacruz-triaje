# vista-urgencias

## MODIFICADO

### Requirement: Puerta de calidad y límites reactivos
`check` mantiene las pruebas existentes con Testcontainers para Kafka y Redis, BlockHound y ArchUnit, y exige cobertura agregada de líneas mínima de 80 % mediante JaCoCo. El cambio agrega las pruebas de la consulta RAG; no agrega exclusiones de cobertura. BlockHound debe vigilar embeddings y llamadas a `ChatModel` simulado, y ArchUnit debe comprobar módulos reales y que dominio/aplicación no importen Spring AI ni Resilience4j.

**Origen en el RFC:** F-05, CA-22, CA-23, CA-24

#### Scenario: Validar la puerta de calidad de la fila
**Origen en el RFC:** CA-24
- **WHEN** se ejecuta `./gradlew check`
- **THEN** pasan las pruebas requeridas, JaCoCo verifica cobertura agregada de líneas de al menos 80 % y el build falla si baja, sin nuevas exclusiones

## NUEVO

### Requirement: Las consultas RAG no bloquean el event loop
Las pruebas de integración con BlockHound cubren las consultas que calculan embeddings y usan un `ChatModel` simulado.

**Origen en el RFC:** RN-10, CA-22

#### Scenario: Detectar bloqueos en las consultas RAG
**Origen en el RFC:** CA-22
- **WHEN** se ejecutan consultas con embeddings y `ChatModel` simulado con BlockHound activo
- **THEN** no se detectan llamadas bloqueantes en hilos del event loop

### Requirement: ArchUnit mantiene aisladas las dependencias de IA
Las reglas arquitectónicas inspeccionan las clases reales de los módulos Gradle y prohíben que dominio y aplicación importen Spring, Spring AI o Resilience4j.

**Origen en el RFC:** RN-10, CA-23

#### Scenario: Comprobar aislamiento de dominio y aplicación
**Origen en el RFC:** CA-23
- **WHEN** corre `ArquitecturaTest` sobre clases reales
- **THEN** dominio y aplicación no importan Spring, Spring AI ni Resilience4j
