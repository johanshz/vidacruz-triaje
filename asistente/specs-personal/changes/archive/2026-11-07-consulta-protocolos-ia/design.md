# Diseño — consulta-protocolos-ia

## Endpoints / puntos de entrada
| Flujo RFC | Método y ruta | Request | Response | Errores |
|---|---|---|---|---|
| F-03 | POST `/asistente/consulta` | JSON `{pregunta}` y `X-API-Key` | 200 `{respuesta, fuentes}`; sin cobertura usa frase fija y fuentes vacías | 400 `VALIDACION`; 401 `NO_AUTORIZADO`; 503 `IA_NO_DISPONIBLE`; 500 `ERROR_INTERNO` |

## Puntos de entrada para el índice
| Método y ruta (o job / evento) | Qué hace | Spec | Cambio |
|---|---|---|---|
| Arranque: CargadorCorpus (carga corpus/PROTO-00N.json e indexa en SimpleVectorStore) | Carga los 3 protocolos (7 secciones), calcula embeddings ONNX locales y falla el arranque si el corpus o el modelo no cargan | corpus-protocolos.md | NUEVO |
| POST `/asistente/consulta` | Consulta protocolos oficiales con RAG | consulta-protocolos.md | NUEVO |
| Ejecución de comparación opcional (prueba con `ANTHROPIC_API_KEY` o script equivalente) | Compara Sonnet 5.5 y Haiku 5.5 en tres consultas | consulta-protocolos.md | NUEVO |

## Paradigma y stack
Java 21, Spring Boot 3.5.16, Gradle 9.2.1. Paradigma reactivo con Spring WebFlux; no se agrega persistencia. Estructura hexagonal con CQRS en español: módulos Gradle `dominio`, `aplicacion`, `infraestructura`. Dominio depende solo de Reactor; aplicación depende de dominio; infraestructura depende de aplicación y dominio. Redis y Kafka vigentes permanecen para la vista de urgencias. Se agregan Spring AI Anthropic, Spring AI Transformers/ONNX, `spring-ai-vector-store` y Resilience4j CircuitBreaker, TimeLimiter y Reactor. El SDK `com.anthropic:anthropic-java` solo se agrega si se activa el plan B tras una falla real de Spring AI.

## Artefactos del modelo y variables de entorno
- Crear `scripts/descargar-modelo.*` para obtener el modelo ONNX y validar un checksum documentado.
- Mantener `model.onnx` y `tokenizer.json` fuera del repositorio mediante `.gitignore`; el modelo no se versiona si pesa más de aproximadamente 100 MB.
- Modificar `Dockerfile` para incluir ambos archivos en la imagen y descargarlos en la etapa de build si faltan.
- Actualizar `.env.example` para declarar las cuatro variables y `docker-compose.yml` para pasar `ANTHROPIC_API_KEY` y `ASISTENTE_MODELO` al servicio.
- Declarar `ANTHROPIC_API_KEY` opcional, `ASISTENTE_MODELO` con default `claude-sonnet-5-5`, `ASISTENTE_UMBRAL_SIMILITUD` con inicial `0.75` y `ASISTENTE_TOP_K` con default `3`, mapeadas a `vidacruz.ia.*`.

## Capas
| Capa | Clases nuevas o tocadas |
|---|---|
| dominio | `protocolo.modelo.entidad.FragmentoProtocolo`; DTO de consulta/respuesta/fuente; `ServicioConsultarProtocolo` (umbral y barreras 2, 4 y 5); puertos `BuscadorProtocolos`, `AsistenteProtocolos`; `ExcepcionIaNoDisponible` |
| aplicacion | `ComandoConsultarProtocolo`, `ManejadorConsultarProtocolo`, `FabricaConsultaProtocolo` |
| infraestructura | `ComandoControladorProtocolo`; adaptadores Spring AI y SimpleVectorStore; cargador de corpus; configuración de embeddings, modelo y Resilience4j; `ManejadorGlobalErrores`; métricas/logs; pruebas de integración y calibración |

## Decisiones
- El dominio contiene reglas y barreras y no depende de Spring AI ni Resilience4j — el RFC exige aislamiento por puertos.
- Embeddings y `ChatClient.call()` bloqueantes se ejecutan en `Schedulers.boundedElastic()` dentro de infraestructura — el RFC prohíbe bloquear el event loop.
- Se usa Spring AI Anthropic primero y el SDK oficial solo si una llamada real falla por incompatibilidad del modelo — decisión explícita en F-03.
- La llamada a Claude no envía `thinking` desactivado ni `temperature` — el modelo rechaza esas opciones (F-03 Modelo).
- Corpus de tres JSON con siete secciones e índice en memoria — no se agrega persistencia.
- `ANTHROPIC_API_KEY` queda opcional al arranque y la consulta sin coincidencia no contacta al proveedor — permite el modo degradado y preservar la vista de urgencias.
- Comparación de modelos no forma parte del build normal; si falta acceso real se produce plantilla y pendiente — F-02 limita gasto a 5 USD y asigna su ejecución a Johan.

## Detalles sin definir (sección 8 del RFC)
| Pendiente | Cómo se deja en el código |
|---|---|
| P-01: modelo final de embeddings y umbral | Implementar valor inicial y calibrar con las tres consultas; documentar el resultado en README y `contexto/tecnico.md` S-08. Si falta modelo/red, dejar calibración explícitamente pendiente y no declarar el índice calibrado. |
| P-02: redacción exacta del prompt | Codex define el prompt en infraestructura; debe limitarse al contexto y exigir JSON `{respuesta, fuentes, sinCobertura}`. |
| P-03: nombres de campos del JSON del corpus | Codex define esquema `codigo`, `titulo`, `secciones[{nombre,texto}]`, con texto literal del corpus oficial. |
| P-04: resultado Sonnet/Haiku | Sonnet permanece predeterminado; generar informe real con clave o plantilla y PE para Johan. No bloquea esta fila. |

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** Plan de propuesta, specs, diseño y tareas para F-01 a F-05.
- **Faltó:** Nada; P-01 a P-04 quedan documentados como decisiones delegadas o pendiente no bloqueante según el RFC.
- **Docs vivos tocados:** Ninguno; solo artefactos del change.
- **Sigue:** `$compara-plan-personal` compara este change contra el RFC; después Codex implementa el plan aprobado.
