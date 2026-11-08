# Informe de plan — consulta-protocolos-ia

Plan generado para el RFC de consulta de protocolos clínicos con RAG. Artefactos: `proposal.md`, `specs/corpus-protocolos.md`, `specs/consulta-protocolos.md`, `specs/observabilidad-consulta.md`, `specs/vista-urgencias.md`, `design.md` y `tasks.md`. La trazabilidad completa está en `reporte-cobertura.md`.

La sección 3 del RFC declara Java 21, Spring Boot 3.5.16, Gradle 9.2.1 y paradigma reactivo WebFlux; se conserva la estructura hexagonal con CQRS existente en los módulos `dominio`, `aplicacion` e `infraestructura`. El change se nombra `consulta-protocolos-ia`; no existía una carpeta de change activa con ese nombre.

No se ejecutaron comandos Gradle ni pruebas: esta skill solo crea el plan. No se modificaron código, RFC, `ESTADO.md` ni documentos vivos.

Corrección del plan en el mismo change: tareas explícitas para descarga/checksum del modelo, `.gitignore`, Dockerfile y variables/Compose; contrato de opciones de Claude y prueba con `ChatModel` simulado; F-04 ya no aparece como punto de entrada propio.

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** Proposal, specs por capacidad, diseño, tareas y reporte de cobertura corregidos en `consulta-protocolos-ia` según el ajuste recibido.
- **Faltó:** Nada del alcance planificable. P-04 requiere posteriormente la comparación de modelos con la clave de Johan; si no está disponible, el RFC manda dejar plantilla y PE, por lo que no bloquea esta fila.
- **Docs vivos tocados:** Ninguno.
- **Sigue:** `$compara-plan-personal` realiza la comparación independiente con el RFC; luego Codex implementa el plan aprobado. No se hizo commit ni push.
