# Tareas â€” asistente-urgencias

## 1. Estructura y build
- [x] 1.1 Agregar dependencias reactivas Redis, Spring Kafka, Spring Security, tracing OTel y dependencias de pruebas indicadas â€” sirve a: F-01, F-02, F-05, F-07
- [x] 1.2 Configurar JaCoCo agregado y umbral de 80 % en `check`, exclusiones Ãºnicamente de `Aplicacion` y `package-info` â€” sirve a: F-07, RN-10, CA-27
- [x] 1.3 Habilitar argumentos JVM requeridos por BlockHound en Java 21 â€” sirve a: F-07, CA-27

## 2. Dominio
- [x] 2.1 Crear excepciones de negocio comunes y de urgencias con sus cÃ³digos y datos de campo â€” sirve a: F-03
- [x] 2.2 Crear catÃ¡logo de niveles y DTO/modelo de evento y estado de urgencias â€” sirve a: F-08, F-09, RN-01, RN-08
- [x] 2.3 Crear puerto y servicio de actualizaciÃ³n con reglas para registrado, alta, consulta y escalado, incluida la validaciÃ³n de tipo, idPaciente y nivel que produce `ExcepcionEventoInvalido` â€” sirve a: F-08, RN-01, RN-02, RN-03, RN-04, RN-05, CA-02, CA-03, CA-04, CA-05, CA-07, CA-08
- [x] 2.4 Crear el puerto `DaoUrgencias` y `DtoEstadoUrgencias` para el resultado de consulta â€” sirve a: F-09, RN-08, CA-12, CA-13
- [x] 2.5 Crear pruebas JUnit 5 + StepVerifier del servicio de actualizaciÃ³n, niveles invÃ¡lidos, eventos ignorados e idempotencia â€” sirve a: RN-01, RN-02, RN-03, RN-05, RN-06, CA-02, CA-03, CA-04, CA-05, CA-07, CA-08, CA-09
- [x] 2.6 Crear pruebas del contrato de `DtoEstadoUrgencias`: cinco niveles en orden fijo, ceros, suma de `totalActivos` y `consultadoEn`, sin servicio de dominio â€” sirve a: RN-08, CA-12, CA-13

## 3. Adaptadores y configuraciÃ³n
- [x] 3.1 Implementar adaptador Redis con SADD y SREM en los cinco conjuntos â€” sirve a: F-08, RN-02, RN-03, CA-01, CA-02, CA-04, CA-05
- [x] 3.2 Implementar adaptador Redis SCARD y traducciÃ³n de error de conexiÃ³n/timeout a `ExcepcionUrgenciasNoDisponible` â€” sirve a: F-09, CA-12, CA-13, CA-14
- [x] 3.3 Configurar propiedades externas Redis y Kafka, grupo, offset, concurrencia, poll y deserializaciÃ³n â€” sirve a: F-01
- [x] 3.4 Declarar DLT, configurar `DefaultErrorHandler`, reintentos 1/2/4 s y excepciones sin reintento â€” sirve a: F-01, F-08, RN-05, RN-07, CA-06, CA-07, CA-08, CA-10, CA-24
- [x] 3.5 Crear consumidor Kafka que filtre versiÃ³n distinta de 1 y tipos desconocidos, y arme `ComandoActualizarUrgencias` para eventos conocidos o incompletos que debe validar dominio; devolver Mono â€” sirve a: F-08, RN-04, RN-06, CA-01, CA-06, CA-09
- [x] 3.6 Configurar seguridad WebFlux, propiedad validada de API key, comparaciÃ³n constante y ruta de error 401 ProblemDetail â€” sirve a: F-02, RN-09, CA-15, CA-16, CA-17
- [x] 3.7 Crear excepciones HTTP globales y ProblemDetail para errores de validaciÃ³n, rutas, mÃ©todos, Redis y errores internos â€” sirve a: F-03, CA-14, CA-18, CA-19, CA-20
- [x] 3.8 Configurar actuator, health details, grupos de probes y exposiciÃ³n de info/prometheus â€” sirve a: F-04, CA-16, CA-21
- [x] 3.9 Configurar tracing OTel sin exportador, formato ECS, propagaciÃ³n Reactor/Kafka y cabecera X-Trace-Id â€” sirve a: F-05, CA-11, CA-22, CA-23
- [x] 3.10 Ajustar `ArquitecturaTest` para importar clases reales por mÃ³dulo y hacer efectivas las reglas â€” sirve a: F-07, RN-10, CA-28
- [x] 3.11 Crear pruebas unitarias Mockito para manejadores de aplicaciÃ³n y errores de adaptadores traducidos â€” sirve a: F-03, F-08, F-09, CA-14, CA-20

## 4. Infraestructura: controlador y configuraciÃ³n de entrada
- [x] 4.1 Crear controlador GET de consulta de urgencias y mapeo del DTO de respuesta â€” sirve a: F-09, RN-08, CA-12, CA-13
- [x] 4.2 Completar rutas actuator y respuestas con X-Trace-Id, incluidas 401/404 â€” sirve a: F-02, F-04, F-05, CA-15, CA-16, CA-18, CA-21, CA-22
- [x] 4.3 Configurar `application.yaml` con variables de entorno y valores predeterminados permitidos por el RFC â€” sirve a: F-01, F-04, F-05
- [x] 4.4 Crear pruebas WebTestClient para contrato GET, seguridad, errores, probes, traceId y readiness â€” sirve a: F-02, F-03, F-04, F-05, F-09, CA-12, CA-13, CA-14, CA-15, CA-16, CA-18, CA-19, CA-20, CA-21, CA-22

## 5. IntegraciÃ³n y entorno
- [x] 5.1 Crear pruebas de integraciÃ³n con Kafka y Redis Testcontainers para DP3, offset, DLT, reintentos y trazas â€” sirve a: F-01, F-05, F-08, CA-01, CA-02, CA-03, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-10, CA-11, CA-24
- [x] 5.2 Instalar BlockHound en pruebas de integraciÃ³n y verificar ejecuciÃ³n reactiva â€” sirve a: F-07, RN-10, CA-27
- [x] 5.3 Crear Dockerfile multietapa con usuario no root y fin de lÃ­nea LF para wrapper â€” sirve a: F-06
- [x] 5.4 Crear Compose con Redis AOF/volumen, Kafka KRaft, healthchecks y puerto publicado 8082; crear `triaje.eventos` con 3 particiones para pruebas locales mediante un servicio o comando de inicio (el servicio Asistente no lo crea) â€” sirve a: F-06, CA-25, CA-26
- [x] 5.5 Crear `.env.example` y asegurar `.env` en `.gitignore` â€” sirve a: F-01, F-02, F-06
- [x] 5.6 Documentar variables, arranque, evento DP3, consulta, probes, errores y supuestos S-10/S-11 en README â€” sirve a: F-06, F-08, F-09
- [x] 5.7 Verificar flujo Compose y persistencia de la vista al reiniciar Redis â€” sirve a: F-06, CA-25, CA-26


