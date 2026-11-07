# camino-principal-paciente

## Por qué
El Triage necesita completar el camino principal de atención: registrar y clasificar pacientes, consultar su ficha, cambiar su estado y publicar los eventos confirmados en el outbox para que el Asistente construya su vista de urgencias.

## Qué cambia
- F-01: registrar y clasificar pacientes, asignar SLA y persistir ficha, auditoría y evento de forma atómica.
- F-02: consultar la ficha completa y el tiempo transcurrido en el estado actual.
- F-03: cambiar el estado solo mediante transiciones válidas, con auditoría y evento atómicos.
- F-04: publicar eventos pendientes del outbox en `triaje.eventos`, conservando orden y tolerancia a fallos.
- RN-01 a RN-13 y CA-01 a CA-28: reglas de clasificación, validación, concurrencia, métricas, calidad y documentación de prueba.

## Qué NO cambia
No se implementan escalamiento por SLA, job de vigilancia, alertas SSE, reporte RF-05, el Asistente, despliegue Kubernetes/Azure ni trazas OTLP. No se agrega migración de base de datos ni se actualiza OpenAPI en esta entrega. El campo `escalado` se devuelve siempre como `false`.

## RFC de origen
C:\Users\Johan Sanchez\Documents\WORKSPACE_IA\proyectos\ceiba-prueba\desarrollo-ia\triage\rfc-activo.md
