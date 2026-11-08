# ADR-08 — Repositorio de entrega: dos repos de trabajo, un monorepo final

- **Fecha:** 2026-11-06
- **Estado:** Aceptada

## Contexto
El enunciado pide **un** repositorio GitHub con todo el código y valora el historial de commits
progresivo. El entorno de trabajo (orquestador y ciclos de fix/refactor) espera un repo git por
componente en `codigo/<Componente>/` (`ciclo.py` exige `.git` en esa carpeta).

## Decisión
- Desarrollo en `codigo/Triage/` y `codigo/Asistente/`, cada uno su repo, con el orquestador sin cambios.
- La fila final del plan crea `codigo/Entrega/` y une los dos con `git subtree add` **sin
  `--squash`** (`triage/` y `asistente/`), lo que conserva cada commit. Ahí se agregan
  `docker-compose.yml`, `deploy/k8s/`, `deploy/azure/`, `docs/adr/` (copia de estos ADR) y el
  `README.md`. Ese repo es el que se sube a GitHub.

## Opciones descartadas
| Opción | Por qué no |
|---|---|
| Monorepo desde el inicio | Mezcla el historial de dos servicios que evolucionan por separado; se pierde el orden de cada uno |
| Un solo proyecto "VidaCruz" con los dos servicios dentro | Cada servicio tiene su propio ciclo de construcción y pruebas; juntarlos desde el inicio los acopla |
| Entregar dos repos | El enunciado dice "repositorio", en singular |

## Consecuencias
- El historial progresivo llega completo al repo entregado.
- La fila final no la corre el orquestador (no es un servicio Java con cobertura): es un
  encargo directo a Codex.

## Cómo se defiende
"Trabajé un repo por servicio y los uní con subtree conservando el historial: el evaluador ve
cada commit de cada servicio."
