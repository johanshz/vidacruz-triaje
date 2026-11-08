# Informe — cierre consulta-protocolos-ia — 2026-11-07

## Trabajo realizado
- 5.5: test de `AdaptadorAsistenteClaude` con `ChatModel` simulado y el `CircuitBreaker`/`TimeLimiter` reales creados por `ConfiguracionAsistenteProtocolos`. Cubre timeout, error de conexión, 500, 429, 401 y 403; todos producen `ExcepcionIaNoDisponible`. Confirma seis fallos contabilizados, apertura corta sin otra llamada al modelo y el timer de error/circuito abierto.
- 5.6: test de `SimpleMeterRegistry` cubre `asistente.llm.latencia` (`ok`, `error`, `circuito_abierto`) y el contador de consultas (`respondida`, `sin_cobertura`, `barrera_rechazada`, `no_disponible`). `ListAppender` confirma que los logs no incluyen pregunta ni respuesta.
- 5.10: captura el `Prompt` enviado al `ChatModel` y confirma que no incluye `temperature` ni `thinking` desactivado.
- Las tres tareas quedaron marcadas `[x]` en `tasks.md`; el change se archivó como `specs-personal/changes/archive/2026-11-07-consulta-protocolos-ia`.

## Verificación
- Clases focalizadas: `AdaptadorAsistenteClaudeTest` y `MetricasConsultaProtocoloTest`, BUILD SUCCESSFUL.
- Suite completa y agregación: `gradlew.bat check`, BUILD SUCCESSFUL.
- JaCoCo agregado: 480/533 líneas cubiertas, 90,06 %.
- CA-22 con ONNX real continúa omitida explícitamente: faltan `model.onnx` y `tokenizer.json`. RFC permite esa omisión con PE. La comparación Sonnet/Haiku también queda pendiente de clave Anthropic; Sonnet sigue como modelo por defecto.
- No se modificó el adaptador de producción ni el RFC, no se alteró `ESTADO.md`/`estado.json` y no se hizo commit/push.

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** pruebas nuevas aprobadas; todas las tareas del change marcadas; change archivado; cobertura JaCoCo sobre el umbral.
- **Faltó:** ejecutar calibración ONNX y comparación de modelos cuando Johan tenga el artefacto y la clave.
- **Docs vivos tocados:** `tasks.md`, `README.md`, `contexto/tecnico.md` y este informe.
- **Sigue:** el orquestador hace el commit; Johan resuelve los PE externos.
