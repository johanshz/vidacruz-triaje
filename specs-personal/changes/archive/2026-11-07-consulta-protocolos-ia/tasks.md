# Tareas â€” consulta-protocolos-ia

## 1. Estructura y configuraciÃ³n
- [x] 1.1 Agregar dependencias Spring AI Anthropic, Transformers/ONNX, vector store y Resilience4j a los mÃ³dulos Gradle existentes â€” sirve a: F-01, F-03
- [x] 1.2 Declarar `ANTHROPIC_API_KEY` como opcional, `ASISTENTE_MODELO` con valor por defecto `claude-sonnet-5-5`, `ASISTENTE_UMBRAL_SIMILITUD` con valor inicial `0.75` y `ASISTENTE_TOP_K` con valor `3`, mapeadas a sus propiedades â€” sirve a: F-01, RFC Â§3, RN-08, CA-12
- [x] 1.3 Crear los tres JSON del corpus con las siete secciones literales de `contexto/funcional.md` â€” sirve a: F-01, CA-18
- [x] 1.4 Crear `scripts/descargar-modelo.*` para obtener el modelo de embeddings y verificar su checksum documentado â€” sirve a: F-01, RFC Â§3, RN-08
- [x] 1.5 Agregar `model.onnx` y `tokenizer.json` a `.gitignore`; no incluir el modelo en el repositorio si supera aproximadamente 100 MB â€” sirve a: F-01, RFC Â§3, RN-08
- [x] 1.6 Modificar `Dockerfile` para empacar `model.onnx` y `tokenizer.json`, descargÃ¡ndolos durante la etapa de build si faltan â€” sirve a: F-01, RFC Â§3, RN-08
- [x] 1.7 Actualizar `.env.example` para declarar las cuatro variables nuevas y `docker-compose.yml` para pasar `ANTHROPIC_API_KEY` y `ASISTENTE_MODELO` al servicio â€” sirve a: F-01, RFC Â§3, RN-08

## 2. Dominio y aplicaciÃ³n
- [x] 2.1 Crear DTO, entidad `FragmentoProtocolo` y puertos `BuscadorProtocolos` y `AsistenteProtocolos` â€” sirve a: F-01, F-03, CA-18
- [x] 2.2 Implementar servicio de consulta, umbral, topK y barreras de cobertura, citas y cifras â€” sirve a: RN-01, RN-02, RN-03, RN-04, RN-06, RN-07, CA-03 a CA-09, CA-14 a CA-16
- [x] 2.3 Crear comando, fÃ¡brica y manejador CQRS de consulta â€” sirve a: F-03, CA-01 a CA-17
- [x] 2.4 Agregar `ExcepcionIaNoDisponible` y mapeo a ProblemDetail 503 en infraestructura â€” sirve a: RN-05, CA-10 a CA-13

## 3. Adaptadores
- [x] 3.1 Implementar cargador de corpus, fragmentaciÃ³n, metadatos e Ã­ndice `SimpleVectorStore` con embeddings ONNX â€” sirve a: F-01, CA-18, CA-19
- [x] 3.2 Ejecutar embeddings en `boundedElastic` y calibrar el umbral con el modelo multilingÃ¼e real; registrar PE explÃ­cito si no estÃ¡ disponible â€” sirve a: F-01, RN-10, CA-19, CA-22
- [x] 3.3 Implementar adaptador Spring AI Anthropic detrÃ¡s de `AsistenteProtocolos`, llamada en `boundedElastic` y uso del JSON requerido; no enviar `thinking` desactivado ni `temperature`; activar SDK alternativo solo ante falla real comprobada â€” sirve a: F-03 (Modelo), RN-06, RN-08, CA-06, CA-12
- [x] 3.4 Configurar TimeLimiter y CircuitBreaker dentro del adaptador, con parÃ¡metros RFC y sin reintentos â€” sirve a: F-03, RN-05, CA-10, CA-11
- [x] 3.5 Agregar timer, contador y logs seguros sin texto de pregunta/respuesta â€” sirve a: F-04, RN-09, CA-20, CA-21

## 4. Entry points y documentaciÃ³n
- [x] 4.1 Implementar `POST /asistente/consulta`, validaciÃ³n del body, API key vigente y mapeo de respuestas/errores â€” sirve a: F-03, RN-07, CA-01 a CA-17
- [x] 4.2 Documentar mÃ©tricas nuevas en endpoint Prometheus existente â€” sirve a: F-04, CA-20
- [x] 4.3 Documentar en README consultas curl, variables, modelo, barreras, degradaciÃ³n, S-15 a S-19 y umbral calibrado â€” sirve a: F-01, F-03, F-04, CA-19
- [x] 4.4 Crear prueba/script opcional de comparaciÃ³n Sonnet/Haiku; producir `comparacion-modelos.md` con resultados o plantilla pendiente y PE para Johan â€” sirve a: F-02, CA-25
- [x] 4.5 Actualizar `contexto/tecnico.md` Â§3 y S-08 con los datos implementados y el umbral/modelo calibrados; si no se calibrÃ³, dejar constancia pendiente â€” sirve a: F-01, CA-19

## 5. Tests unitarios y de integraciÃ³n
- [x] 5.1 Probar cada barrera, cifras y equivalencias, fuentes vÃ¡lidas/inventadas, sin coincidencia sin interacciÃ³n con el puerto LLM, lÃ­mites de entrada â€” JUnit 5 y StepVerifier â€” sirve a: CA-03 a CA-09, CA-14 a CA-16
- [x] 5.2 Probar manejador CQRS y puertos con Mockito â€” sirve a: F-03, CA-01 a CA-17
- [x] 5.3 Probar corpus de siete secciones, recuperaciÃ³n de consultas y calibraciÃ³n ONNX real; usar omisiÃ³n explÃ­cita y PE cuando falte el modelo â€” sirve a: CA-18, CA-19
- [x] 5.4 Probar endpoint con WebTestClient y ChatModel simulado: consultas de ejemplo, 400, 401, 503, vista de urgencias y readiness â€” sirve a: CA-01, CA-02, CA-04, CA-10 a CA-17
- [x] 5.5 Probar CircuitBreaker ante timeout, conexiÃ³n, 5xx, 429, 401 y 403, incluyendo latencia menor a dos segundos al abrir â€” sirve a: CA-10, CA-11
- [x] 5.6 Probar mÃ©tricas de cada resultado y confirmar que logs nunca contienen pregunta o respuesta â€” sirve a: CA-20, CA-21
- [x] 5.7 Ejecutar pruebas con BlockHound para embeddings y ChatModel simulado â€” sirve a: RN-10, CA-22
- [x] 5.8 Actualizar `ArquitecturaTest` para clases reales y aislamiento de Spring/Spring AI/Resilience4j en dominio y aplicaciÃ³n â€” sirve a: RN-10, CA-23
- [x] 5.9 Ajustar pruebas de la puerta de calidad modificada y verificar `./gradlew check` con cobertura agregada JaCoCo mÃ­nima de 80 %, sin nuevas exclusiones â€” sirve a: F-05, CA-24
- [x] 5.10 Probar con `ChatModel` simulado que la peticiÃ³n al proveedor no incluye la opciÃ³n `thinking` desactivado ni `temperature` â€” sirve a: F-03 (Modelo)

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** las tareas 1.2 a 5.10 quedaron verificadas y marcadas. Se agregaron las pruebas 5.5, 5.6 y 5.10; las pruebas focalizadas y `check` completo pasaron. La cobertura agregada JaCoCo es 90,06 % (480/533 líneas).
- **Faltó:** calibración con ONNX multilingüe real y comparación Sonnet/Haiku con clave Anthropic; permanecen como PE explícitos de Johan conforme a F-01/F-02/F-05.
- **Docs vivos tocados:** `README.md`, `contexto/tecnico.md` (S-08), este change y el informe de cierre. No se modificaron RFC, `ESTADO.md` ni `estado.json`.
- **Sigue:** Johan resuelve los PE de ONNX/Anthropic; el orquestador realiza el commit pendiente.