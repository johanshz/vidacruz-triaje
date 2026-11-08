# Informe — continuación del change consulta-protocolos-ia — 2026-11-07

## Resultado de verificación
Se revisaron las tareas 1.2 a 5.10 contra código, pruebas, configuración y documentación. Las tareas implementadas o permitidas como pendientes explícitos quedaron marcadas en `tasks.md`. No se ejecutó Gradle, de acuerdo con la instrucción de reanudación; se conserva la evidencia del último `check` exitoso (80,67 %) en `informes/fixes-20261107.md`.

## PE registrados
- **PE pendiente de Johan — tareas 3.2, 4.3, 4.5, 5.3 y 5.7:** obtener `model.onnx` y `tokenizer.json`, calibrar con las tres consultas, ejecutar la integración condicional con BlockHound y documentar el umbral final. S-08 y README indican que 0.75 es provisional y que no se declara calibrado.
- **PE pendiente de Johan — tarea 4.4:** ejecutar la comparación Sonnet/Haiku con `ANTHROPIC_API_KEY` y completar `desarrollo-ia/asistente/comparacion-modelos.md`. Sonnet sigue por defecto.

## Tareas que impiden cerrar el change
- **5.5:** no hay pruebas del CircuitBreaker para timeout, conexión, 5xx, 429, 401 y 403 ni de apertura en menos de 2 s.
- **5.6:** no se prueban todos los resultados métricos ni que los logs excluyan pregunta y respuesta.
- **5.10:** no hay prueba que inspeccione que el request al proveedor omite `thinking` y `temperature`.

Estas tres tareas no dependen de ONNX ni de credenciales. Por eso se mantienen sin marcar y el change queda activo; no se archiva.

## Cierre
- **Estado:** DETENIDO
- **Hecho:** 27 tareas pendientes revisadas se marcaron completadas; se agregó cobertura de 401 en `/asistente/consulta`, se actualizó S-08 y se dejaron explícitos los PE de Johan.
- **Faltó:** cerrar 5.5, 5.6 y 5.10.
- **Docs vivos tocados:** `contexto/tecnico.md`, `README.md`, `specs-personal/changes/consulta-protocolos-ia/tasks.md`, este informe y `desarrollo-ia/_orquestador-asistente/preguntas-respuestas.md`.
- **Sigue:** Codex completa las tres tareas; el orquestador incorpora pendientes en `ESTADO.md`, ejecuta el cierre normal y hace el commit.
