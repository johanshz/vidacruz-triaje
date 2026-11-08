# Cobertura del RFC — consulta-protocolos-ia

RFC: `desarrollo-ia/asistente/rfc-activo.md`

## Flujos
| ID | Artefactos del plan | Cobertura |
|---|---|---|
| F-01 | `specs/corpus-protocolos.md`, design, tasks 1.1–1.7, 3.1–3.2, 4.3, 4.5, 5.3 | Completa: corpus, siete fragmentos, descarga/checksum e inclusión del modelo, variables, ONNX, índice, calibración y pendiente explícito |
| F-02 | `specs/consulta-protocolos.md`, design, tasks 4.4 | Completa: comparación Sonnet/Haiku, criterio, límite de gasto y plantilla/PE si no hay acceso |
| F-03 | `specs/consulta-protocolos.md`, design, tasks 2.1–2.4, 3.3–3.4, 4.1, 5.1–5.5 | Completa: contrato, validación, cinco barreras, resiliencia y modo degradado |
| F-04 | `specs/observabilidad-consulta.md`, design, tasks 3.5, 4.2, 5.6 | Completa: timer, contador en el Prometheus ya existente y logs sin texto clínico; F-04 no agrega una entrada propia al índice |
| F-05 | `specs/vista-urgencias.md`, tasks 5.7–5.9 | Completa: requisito de calidad modificado y criterios de BlockHound, ArchUnit y JaCoCo |

## Reglas de negocio
| IDs | Dónde aparecen |
|---|---|
| RN-01–RN-04, RN-06–RN-08, RN-10 | `specs/consulta-protocolos.md`; RN-10 también en `specs/corpus-protocolos.md` y `specs/vista-urgencias.md` |
| RN-05 | `specs/consulta-protocolos.md` (modo degradado) |
| RN-09 | `specs/observabilidad-consulta.md` |

## Criterios de aceptación
| IDs | Dónde aparecen |
|---|---|
| CA-01–CA-17 | `specs/consulta-protocolos.md` |
| CA-18–CA-19 | `specs/corpus-protocolos.md` |
| CA-20–CA-21 | `specs/observabilidad-consulta.md` |
| CA-22–CA-24 | `specs/vista-urgencias.md` |
| CA-25 | `specs/consulta-protocolos.md` |

## Detalles pendientes
P-01 a P-03 quedan resueltos por decisiones/tareas delegadas a Codex descritas en `design.md`. P-04 no bloquea el plan: Sonnet permanece predeterminado hasta que Johan ejecute la comparación; `tasks.md` exige dejar resultados o plantilla y PE. Ningún RN o CA quedó sin spec ni tarea.

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** Matriz de trazabilidad de los cinco flujos, diez reglas de negocio y veinticinco criterios de aceptación.
- **Faltó:** Nada; P-04 queda como pendiente explícito y no bloqueante asignado a Johan.
- **Docs vivos tocados:** Ninguno; este archivo pertenece al change.
- **Sigue:** Comparar independientemente con `$compara-plan-personal`.
