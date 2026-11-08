# Tareas — asistente-urgencias

## 1. Estructura y build
- [x] 1.1 Agregar dependencias reactivas Redis, Spring Kafka, Spring Security, tracing OTel y dependencias de pruebas indicadas — sirve a: F-01, F-02, F-05, F-07
- [x] 1.2 Configurar JaCoCo agregado y umbral de 80 % en `check`, exclusiones únicamente de `Aplicacion` y `package-info` — sirve a: F-07, RN-10, CA-27
- [x] 1.3 Habilitar argumentos JVM requeridos por BlockHound en Java 21 — sirve a: F-07, CA-27

## 2. Dominio
- [x] 2.1 Crear excepciones de negocio comunes y de urgencias con sus códigos y datos de campo — sirve a: F-03
- [x] 2.2 Crear catálogo de niveles y DTO/modelo de evento y estado de urgencias — sirve a: F-08, F-09, RN-01, RN-08
- [x] 2.3 Crear puerto y servicio de actualización con reglas para registrado, alta, consulta y escalado, incluida la validación de tipo, idPaciente y nivel que produce `ExcepcionEventoInvalido` — sirve a: F-08, RN-01, RN-02, RN-03, RN-04, RN-05, CA-02, CA-03, CA-04, CA-05, CA-07, CA-08
- [x] 2.4 Crear el puerto `DaoUrgencias` y `DtoEstadoUrgencias` para el resultado de consulta — sirve a: F-09, RN-08, CA-12, CA-13
- [x] 2.5 Crear pruebas JUnit 5 + StepVerifier del servicio de actualización, niveles inválidos, eventos ignorados e idempotencia — sirve a: RN-01, RN-02, RN-03, RN-05, RN-06, CA-02, CA-03, CA-04, CA-05, CA-07, CA-08, CA-09
- [x] 2.6 Crear pruebas del contrato de `DtoEstadoUrgencias`: cinco niveles en orden fijo, ceros, suma de `totalActivos` y `consultadoEn`, sin servicio de dominio — sirve a: RN-08, CA-12, CA-13

## 3. Adaptadores y configuración
- [x] 3.1 Implementar adaptador Redis con SADD y SREM en los cinco conjuntos — sirve a: F-08, RN-02, RN-03, CA-01, CA-02, CA-04, CA-05
- [x] 3.2 Implementar adaptador Redis SCARD y traducción de error de conexión/timeout a `ExcepcionUrgenciasNoDisponible` — sirve a: F-09, CA-12, CA-13, CA-14
- [x] 3.3 Configurar propiedades externas Redis y Kafka, grupo, offset, concurrencia, poll y deserialización — sirve a: F-01
- [x] 3.4 Declarar DLT, configurar `DefaultErrorHandler`, reintentos 1/2/4 s y excepciones sin reintento — sirve a: F-01, F-08, RN-05, RN-07, CA-06, CA-07, CA-08, CA-10, CA-24
- [x] 3.5 Crear consumidor Kafka que filtre versión distinta de 1 y tipos desconocidos, y arme `ComandoActualizarUrgencias` para eventos conocidos o incompletos que debe validar dominio; devolver Mono — sirve a: F-08, RN-04, RN-06, CA-01, CA-06, CA-09
- [x] 3.6 Configurar seguridad WebFlux, propiedad validada de API key, comparación constante y ruta de error 401 ProblemDetail — sirve a: F-02, RN-09, CA-15, CA-16, CA-17
- [x] 3.7 Crear excepciones HTTP globales y ProblemDetail para errores de validación, rutas, métodos, Redis y errores internos — sirve a: F-03, CA-14, CA-18, CA-19, CA-20
- [x] 3.8 Configurar actuator, health details, grupos de probes y exposición de info/prometheus — sirve a: F-04, CA-16, CA-21
- [x] 3.9 Configurar tracing OTel sin exportador, formato ECS, propagación Reactor/Kafka y cabecera X-Trace-Id — sirve a: F-05, CA-11, CA-22, CA-23
- [x] 3.10 Ajustar `ArquitecturaTest` para importar clases reales por módulo y hacer efectivas las reglas — sirve a: F-07, RN-10, CA-28
- [x] 3.11 Crear pruebas unitarias Mockito para manejadores de aplicación y errores de adaptadores traducidos — sirve a: F-03, F-08, F-09, CA-14, CA-20

## 4. Infraestructura: controlador y configuración de entrada
- [x] 4.1 Crear controlador GET de consulta de urgencias y mapeo del DTO de respuesta — sirve a: F-09, RN-08, CA-12, CA-13
- [x] 4.2 Completar rutas actuator y respuestas con X-Trace-Id, incluidas 401/404 — sirve a: F-02, F-04, F-05, CA-15, CA-16, CA-18, CA-21, CA-22
- [x] 4.3 Configurar `application.yaml` con variables de entorno y valores predeterminados permitidos por el RFC — sirve a: F-01, F-04, F-05
- [x] 4.4 Crear pruebas WebTestClient para contrato GET, seguridad, errores, probes, traceId y readiness — sirve a: F-02, F-03, F-04, F-05, F-09, CA-12, CA-13, CA-14, CA-15, CA-16, CA-18, CA-19, CA-20, CA-21, CA-22

## 5. Integración y entorno
- [x] 5.1 Crear pruebas de integración con Kafka y Redis Testcontainers para DP3, offset, DLT, reintentos y trazas — sirve a: F-01, F-05, F-08, CA-01, CA-02, CA-03, CA-04, CA-05, CA-06, CA-07, CA-08, CA-09, CA-10, CA-11, CA-24
- [x] 5.2 Instalar BlockHound en pruebas de integración y verificar ejecución reactiva — sirve a: F-07, RN-10, CA-27
- [x] 5.3 Crear Dockerfile multietapa con usuario no root y fin de línea LF para wrapper — sirve a: F-06
- [x] 5.4 Crear Compose con Redis AOF/volumen, Kafka KRaft, healthchecks y puerto publicado 8082; crear `triaje.eventos` con 3 particiones para pruebas locales mediante un servicio o comando de inicio (el servicio Asistente no lo crea) — sirve a: F-06, CA-25, CA-26
- [x] 5.5 Crear `.env.example` y asegurar `.env` en `.gitignore` — sirve a: F-01, F-02, F-06
- [x] 5.6 Documentar variables, arranque, evento DP3, consulta, probes, errores y supuestos S-10/S-11 en README — sirve a: F-06, F-08, F-09
- [x] 5.7 Verificar flujo Compose y persistencia de la vista al reiniciar Redis — sirve a: F-06, CA-25, CA-26


