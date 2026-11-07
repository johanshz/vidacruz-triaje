# vigilancia-sla-alertas-reporte

## Por qué
Los pacientes que superan su SLA deben escalarse una sola vez, informar al equipo médico en segundos y alimentar un reporte operativo horario.

## Qué cambia
- F-01: configura el SLA aplicado al registro mediante propiedades validadas al arrancar.
- F-02: escala pacientes vencidos con auditoría, outbox, transacción y métrica.
- F-03: consume eventos de escalamiento y publica alertas por instancia.
- F-04: expone alertas iniciales y nuevas mediante SSE protegido.
- F-05: expone reporte horario calculado por SQL.
- RN-01 a RN-18 y CA-01 a CA-30: reglas y criterios asociados a los flujos y calidad.

## Qué NO cambia
Esquema Flyway V1, endpoints existentes salvo el SLA del registro, publicador del outbox, seguridad base, readiness, Asistente y despliegue final. No se añaden dependencias productivas ni adaptadores externos de notificación. OpenAPI queda para `$documentar-api`.

## RFC de origen
C:\Users\Johan Sanchez\Documents\WORKSPACE_IA\proyectos\ceiba-prueba\desarrollo-ia\triage\rfc-activo.md

## Cierre
- **Estado:** COMPLETADO
- **Hecho:** Plan creado con proposal, cinco specs, diseño y tareas trazables al RFC.
- **Faltó:** nada; los detalles sin definir P-01 a P-04 se conservan como decisiones locales o pendientes según el RFC.
- **Docs vivos tocados:** ninguno.
- **Sigue:** comparar plan contra RFC con `$compara-plan-personal`; después Johan decide su aprobación.
