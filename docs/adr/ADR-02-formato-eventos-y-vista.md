# ADR-02 — Formato de los eventos y vista de urgencias idempotente

- **Fecha:** 2026-11-06
- **Estado:** Aceptada

## Contexto
El enunciado deja libre el formato del mensaje en `triaje.eventos` y pide documentarlo. El
Asistente reconstruye los activos por nivel solo con esos eventos (RF-04 B). Con outbox
(ADR-03) la entrega es "al menos una vez": un evento puede llegar repetido.

## Decisión
1. **JSON con sobre propio**, igual para los 4 tipos:
   ```json
   { "idEvento": "uuid", "tipo": "paciente_registrado", "version": 1,
     "ocurridoEn": "2026-11-08T10:00:00Z",
     "datos": { "idPaciente": "uuid", "nivel": "URGENTE", "estadoAnterior": null,
                "estadoNuevo": "REGISTRADO", "escalado": false } }
   ```
2. **Llave del mensaje = `idPaciente`**: todos los eventos de un paciente caen en la misma
   partición y llegan en orden.
3. **Vista en Redis con un conjunto por nivel:** `paciente_registrado` → `SADD
   urgencias:nivel:<NIVEL> <idPaciente>`; `paciente_alta` → `SREM`; `en_consulta` y `escalado`
   no cambian el conteo. `GET /asistente/estado-urgencias` = `SCARD` de cada nivel.

## Opciones descartadas
| Opción | Por qué no |
|---|---|
| CloudEvents | Estándar útil entre organizaciones; aquí suma una librería sin beneficio para 2 servicios propios |
| Avro + registro de esquemas | Un contenedor más en local y en Azure; el contrato es pequeño y estable |
| Contadores (`INCR`/`DECR`) | Un evento repetido cuenta doble; habría que guardar los `idEvento` procesados |

## Consecuencias
- Un evento repetido no altera la vista (`SADD`/`SREM` son idempotentes).
- `version` permite evolucionar el contrato sin romper al consumidor.
- El contrato vive en el README (sin registro de esquemas que lo valide).

## Cómo se defiende
"JSON legible con sobre versionado; la llave por paciente da orden, y los conjuntos de Redis
hacen la vista idempotente sin guardar eventos procesados."
