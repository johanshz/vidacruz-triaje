# Tareas — consulta-protocolos-ia

## 1. Estructura y configuración
- [x] 1.1 Agregar dependencias Spring AI Anthropic, Transformers/ONNX, vector store y Resilience4j a los módulos Gradle existentes — sirve a: F-01, F-03
- [x] 1.2 Declarar `ANTHROPIC_API_KEY` como opcional, `ASISTENTE_MODELO` con valor por defecto `claude-sonnet-5-5`, `ASISTENTE_UMBRAL_SIMILITUD` con valor inicial `0.75` y `ASISTENTE_TOP_K` con valor `3`, mapeadas a sus propiedades — sirve a: F-01, RFC §3, RN-08, CA-12
- [x] 1.3 Crear los tres JSON del corpus con las siete secciones literales de `contexto/funcional.md` — sirve a: F-01, CA-18
- [x] 1.4 Crear `scripts/descargar-modelo.*` para obtener el modelo de embeddings y verificar su checksum documentado — sirve a: F-01, RFC §3, RN-08
- [x] 1.5 Agregar `model.onnx` y `tokenizer.json` a `.gitignore`; no incluir el modelo en el repositorio si supera aproximadamente 100 MB — sirve a: F-01, RFC §3, RN-08
- [x] 1.6 Modificar `Dockerfile` para empacar `model.onnx` y `tokenizer.json`, descargándolos durante la etapa de build si faltan — sirve a: F-01, RFC §3, RN-08
- [x] 1.7 Actualizar `.env.example` para declarar las cuatro variables nuevas y `docker-compose.yml` para pasar `ANTHROPIC_API_KEY` y `ASISTENTE_MODELO` al servicio — sirve a: F-01, RFC §3, RN-08

## 2. Dominio y aplicación
- [x] 2.1 Crear DTO, entidad `FragmentoProtocolo` y puertos `BuscadorProtocolos` y `AsistenteProtocolos` — sirve a: F-01, F-03, CA-18
- [x] 2.2 Implementar servicio de consulta, umbral, topK y barreras de cobertura, citas y cifras — sirve a: RN-01, RN-02, RN-03, RN-04, RN-06, RN-07, CA-03 a CA-09, CA-14 a CA-16
- [x] 2.3 Crear comando, fábrica y manejador CQRS de consulta — sirve a: F-03, CA-01 a CA-17
- [x] 2.4 Agregar `ExcepcionIaNoDisponible` y mapeo a ProblemDetail 503 en infraestructura — sirve a: RN-05, CA-10 a CA-13

## 3. Adaptadores
- [x] 3.1 Implementar cargador de corpus, fragmentación, metadatos e índice `SimpleVectorStore` con embeddings ONNX — sirve a: F-01, CA-18, CA-19
- [x] 3.2 Ejecutar embeddings en `boundedElastic` y calibrar el umbral con el modelo multilingüe real; registrar PE explícito si no está disponible — sirve a: F-01, RN-10, CA-19, CA-22
- [x] 3.3 Implementar adaptador Spring AI Anthropic detrás de `AsistenteProtocolos`, llamada en `boundedElastic` y uso del JSON requerido; no enviar `thinking` desactivado ni `temperature`; activar SDK alternativo solo ante falla real comprobada — sirve a: F-03 (Modelo), RN-06, RN-08, CA-06, CA-12
- [x] 3.4 Configurar TimeLimiter y CircuitBreaker dentro del adaptador, con parámetros RFC y sin reintentos — sirve a: F-03, RN-05, CA-10, CA-11
- [x] 3.5 Agregar timer, contador y logs seguros sin texto de pregunta/respuesta — sirve a: F-04, RN-09, CA-20, CA-21

## 4. Entry points y documentación
- [x] 4.1 Implementar `POST /asistente/consulta`, validación del body, API key vigente y mapeo de respuestas/errores — sirve a: F-03, RN-07, CA-01 a CA-17
- [x] 4.2 Documentar métricas nuevas en endpoint Prometheus existente — sirve a: F-04, CA-20
- [x] 4.3 Documentar en README consultas curl, variables, modelo, barreras, degradación, S-15 a S-19 y umbral calibrado — sirve a: F-01, F-03, F-04, CA-19
- [x] 4.4 Crear prueba/script opcional de comparación Sonnet/Haiku; producir `comparacion-modelos.md` con resultados o plantilla pendiente y PE para Johan — sirve a: F-02, CA-25
- [x] 4.5 Actualizar `contexto/tecnico.md` §3 y S-08 con los datos implementados y el umbral/modelo calibrados; si no se calibró, dejar constancia pendiente — sirve a: F-01, CA-19

## 5. Tests unitarios y de integración
- [x] 5.1 Probar cada barrera, cifras y equivalencias, fuentes válidas/inventadas, sin coincidencia sin interacción con el puerto LLM, límites de entrada — JUnit 5 y StepVerifier — sirve a: CA-03 a CA-09, CA-14 a CA-16
- [x] 5.2 Probar manejador CQRS y puertos con Mockito — sirve a: F-03, CA-01 a CA-17
- [x] 5.3 Probar corpus de siete secciones, recuperación de consultas y calibración ONNX real; usar omisión explícita y PE cuando falte el modelo — sirve a: CA-18, CA-19
- [x] 5.4 Probar endpoint con WebTestClient y ChatModel simulado: consultas de ejemplo, 400, 401, 503, vista de urgencias y readiness — sirve a: CA-01, CA-02, CA-04, CA-10 a CA-17
- [x] 5.5 Probar CircuitBreaker ante timeout, conexión, 5xx, 429, 401 y 403, incluyendo latencia menor a dos segundos al abrir — sirve a: CA-10, CA-11
- [x] 5.6 Probar métricas de cada resultado y confirmar que logs nunca contienen pregunta o respuesta — sirve a: CA-20, CA-21
- [x] 5.7 Ejecutar pruebas con BlockHound para embeddings y ChatModel simulado — sirve a: RN-10, CA-22
- [x] 5.8 Actualizar `ArquitecturaTest` para clases reales y aislamiento de Spring/Spring AI/Resilience4j en dominio y aplicación — sirve a: RN-10, CA-23
- [x] 5.9 Ajustar pruebas de la puerta de calidad modificada y verificar `./gradlew check` con cobertura agregada JaCoCo mínima de 80 %, sin nuevas exclusiones — sirve a: F-05, CA-24
- [x] 5.10 Probar con `ChatModel` simulado que la petición al proveedor no incluye la opción `thinking` desactivado ni `temperature` — sirve a: F-03 (Modelo)

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** las tareas 1.2 a 5.10 quedaron verificadas y marcadas. Se agregaron las pruebas 5.5, 5.6 y 5.10; las pruebas focalizadas y `check` completo pasaron. La cobertura agregada JaCoCo es 90,06 % (480/533 líneas).
- **Faltó:** calibración con ONNX multilingüe real y comparación Sonnet/Haiku con clave Anthropic; permanecen como PE explícitos de Johan conforme a F-01/F-02/F-05.
- **Docs vivos tocados:** `README.md`, `contexto/tecnico.md` (S-08), este change y el informe de cierre. No se modificaron RFC, `ESTADO.md` ni `estado.json`.
- **Sigue:** Johan resuelve los PE de ONNX/Anthropic; el orquestador realiza el commit pendiente.