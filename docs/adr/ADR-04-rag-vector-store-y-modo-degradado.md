# ADR-04 — RAG: librería, proveedor, vector store, anti-alucinación y modo degradado

- **Fecha:** 2026-11-06
- **Estado:** Aceptada

## Contexto
RF-04 A y RN-05: el asistente nunca inventa; toda respuesta clínica cita al menos una fuente
válida del corpus; sin coincidencia, solo el aviso "no existe protocolo disponible". El enunciado
pide circuit breaker con fallback 503 y documentar el vector store en un ADR. Stack: Spring
Boot 3 (Spring AI 2.x exige Boot 4, por eso 1.1.x).

## Decisión
1. **Spring AI 1.1.x + Claude de Anthropic** para redactar la respuesta (reemplaza
   la opción inicial, OpenAI). Clave en `ANTHROPIC_API_KEY`, con límite de gasto en la
   consola. Modelo configurable (`ASISTENTE_MODELO`): `claude-sonnet-5-5`, gama media: redacta con
   fiabilidad en JSON y citando (~0,008 USD por consulta); `claude-haiku-5-5` si la comparación muestra que
   pasa las mismas pruebas. Sin `thinking` desactivado ni `temperature` en la
   petición. Si el módulo Anthropic de Spring AI 1.1 no soporta bien el modelo, el adaptador usa
   el SDK oficial de Java (`com.anthropic`) detrás del mismo puerto.
2. **Embeddings locales ONNX** (`spring-ai-starter-model-transformers`) con un modelo
   multilingüe dentro de la imagen: Anthropic no ofrece API de embeddings, y así la búsqueda no
   depende de la red.
3. **Vector store en memoria** (`SimpleVectorStore`): un fragmento por **sección** de protocolo,
   con metadatos `protocolo` y `seccion`. Se indexa al arrancar (local, en milisegundos); la
   readiness no depende de Claude.
4. **Anti-alucinación en 5 barreras:**
   1. Búsqueda con umbral de similitud configurable (inicial 0.75; calibrado en 0.55 el 2026-11-07 con las 3 consultas
      de ejemplo).
   2. Sin fragmentos sobre el umbral → respuesta fija "No existe un protocolo disponible para
      esta pregunta" con `fuentes: []`, **sin llamar al LLM**.
   3. Prompt que obliga a usar solo el contexto y a devolver JSON `{respuesta, fuentes, sinCobertura}`.
   4. Validación: cada fuente citada debe estar entre los fragmentos recuperados; si falla o
      `sinCobertura = true` → la respuesta fija. Las fuentes de la respuesta se arman desde los
      metadatos, no desde el texto libre del LLM.
   5. Cifras y dosis: cada número de la respuesta (ml/kg, %, mmHg, minutos) debe aparecer en los
      fragmentos recuperados; si no → la respuesta fija. Protege de una dosis cambiada sin
      depender del modelo.
5. **Modo degradado:** `CircuitBreaker` y `TimeLimiter` de Resilience4j (operadores Reactor)
   dentro del adaptador de IA. Circuito abierto o error → `ExcepcionIaNoDisponible` → **503**
   ProblemDetail con `codigo` = `IA_NO_DISPONIBLE` y `detail` = "El asistente clínico no está
   disponible en este momento. Consulte los protocolos impresos".
   Las llamadas bloqueantes de Spring AI van en `Schedulers.boundedElastic()`.

## Opciones descartadas
| Opción | Por qué no |
|---|---|
| LangChain4j | Llamadas bloqueantes igual que Spring AI y fuera del mundo Spring que ya usa el resto |
| OpenAI (decisión inicial) | Ya había una cuenta y clave de Anthropic: no hay que abrir otra cuenta ni pagar otro proveedor |
| Voyage AI para embeddings | Otra cuenta y otra clave externa para 8 fragmentos; los embeddings locales bastan |
| Gemini gratis | Cupo diario bajo y no publicado; si el evaluador lo agota, solo ve el 503 |
| Claude en Azure (Foundry) | Sin confirmar que la cuenta gratis lo habilite; más configuración |
| pgvector | El Asistente no tiene PostgreSQL; agregarlo es otra base para 8 fragmentos |
| Redis como vector store | Requiere el módulo de búsqueda de Redis; más configuración para 8 fragmentos |

## Consecuencias
- Cada réplica indexa al arrancar (local, sin costo). Señal para revisarla: cientos de
  secciones o varias réplicas → pgvector o Redis detrás del mismo puerto `VectorStore`.
- Las 3 consultas de ejemplo son tests automáticos con el LLM simulado: PROTO-001, PROTO-002 y
  "sin protocolo" sin llamar al chat.
- Si Claude cae, `GET /asistente/estado-urgencias` sigue funcionando y "sin protocolo" también responde (la búsqueda es local).

## Cómo se defiende
"Cinco barreras contra la alucinación, en código y no en el modelo; y la más fuerte es no llamar al LLM cuando no hay
protocolo; si la IA cae, el circuito responde 503 en milisegundos y nada más se afecta."
