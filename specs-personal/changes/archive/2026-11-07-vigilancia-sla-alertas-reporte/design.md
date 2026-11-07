# Diseño — vigilancia-sla-alertas-reporte

## Endpoints / puntos de entrada
| Flujo RFC | Método y ruta | Request | Response | Errores |
|---|---|---|---|---|
| F-01 | POST `/triage/pacientes` | Request vigente | 201; `slaMinutos` configurado | Errores actuales; SLA inválido impide arranque |
| F-02 | Job periódico | `ComandoEscalarVencidos` | Sin HTTP; transacción por ciclo | Rollback, log ERROR, reintento siguiente ciclo |
| F-03 | Kafka `triaje.eventos` | Sobre de evento existente | Alerta en sink local | Mensaje mal formado a DLT tras reintentos |
| F-04 | GET `/triage/alertas/flujo` | `X-API-Key` | SSE `text/event-stream` | 401 `NO_AUTORIZADO` |
| F-05 | GET `/triage/reportes/operacion` | `desde`, `hasta` ISO 8601 (`Z` u offset), `X-API-Key` | 200 JSON: `desde`, `hasta`, `zonaHoraria`, `horas[].hora` con offset, `totalRegistrados`, `acumuladoRegistrados` (suma inclusiva de `totalRegistrados` de horas con registros; campo propio del Triage), `porNivel` (5 claves siempre presentes, 0 si no hay), `esperaPromedioMinutos` y `esperaP95Minutos` con 1 decimal (`ROUND(...::numeric,1)`) o null | 400 `VALIDACION`; 401 `NO_AUTORIZADO` |

## Puntos de entrada para el índice
| Método y ruta (o job / evento) | Qué hace | Spec | Cambio |
|---|---|---|---|
| POST `/triage/pacientes` | Registra con SLA de propiedades | paciente.md | MODIFICADO |
| Job vigilancia (`ComandoEscalarVencidos`) | Escala fichas vencidas y escribe auditoría/outbox | vigilancia.md | NUEVO |
| Kafka `triaje.eventos` (`paciente_escalado`) | Convierte el evento en alerta local | alertas.md | NUEVO |
| GET `/triage/alertas/flujo` | Emite estado inicial y alertas nuevas por SSE | alertas.md | NUEVO |
| GET `/triage/reportes/operacion` | Consulta reporte horario | reporte-operacion.md | NUEVO |

## Paradigma y stack
Java 21, Spring Boot 3.5.16, Gradle 9.2.1; reactivo con Spring WebFlux y R2DBC, PostgreSQL 16 y Flyway ya instalado. Estructura hexagonal + CQRS de Ceiba en español, módulos `dominio`, `aplicacion`, `infraestructura`; no usar scaffold Bancolombia ni añadir dependencias productivas. Kafka existente. Sin nueva migración. `Clock` UTC inyectable.

## Capas
| Capa | Clases nuevas o tocadas |
|---|---|
| dominio | Configuración SLA sin Spring; servicio de escalamiento; puertos de escalamiento, `DaoAlerta`, `NotificadorAlerta`, `DaoReporteOperacion`; DTOs y validación reporte |
| aplicacion | `ComandoEscalarVencidos`, manejador; `ComandoEmitirAlerta`, manejador; `ManejadorConsultarReporteOperacion` |
| infraestructura | `controlador`: consultas SSE y reporte; `consumidor`: Kafka y memoria de duplicados; `tarea`: job programado no solapable; `adaptador.repositorio`: UPDATE condicional y auditoría/outbox reutilizados; `adaptador.dao` (+ MapRow): consultas PostgreSQL de alerta y reporte; `adaptador.sse`: sink multicast; `configuracion`: propiedades SLA, Kafka y reporte |


## Decisiones
- Reutilizar V1 y los puertos existentes de auditoría, outbox, transacción y publicador — el RFC prohíbe nueva migración y dependencia productiva.
- UPDATE condicional `REGISTRADO`, `escalado=false` y SLA vencido como exclusión concurrente entre réplicas — contrato F-02.
- Query de reporte en un único `DatabaseClient` con agregados filtrados, `percentile_cont` y suma de ventana — RN-16.
- P-01: fijar tamaños razonables configurables para buffer y memoria de IDs; documentarlos en configuración.
- P-02: la creación/disponibilidad de DLT depende de Kafka/compose de la fila 6; el consumidor configura la publicación a DLT.
- P-03: nombres y módulos se eligen respetando las capas descritas; duplicados se mantienen en adaptador de consumidor con memoria acotada.
- P-04: fecha inicial ISO 8601 UTC de auditoría; minutos enteros truncados hacia abajo, como ya concreta el RFC.

## Detalles sin definir (sección 8 del RFC)
| Pendiente | Cómo se deja en el código |
|---|---|
| P-01 Buffer y caché de duplicados; política ante lento | Valores razonables configurables; descartar o cortar al lento sin afectar otros |
| P-02 DLT aún no creada (Johan, fila 6) | Configurar destino DLT; creación/auto-creación queda a infraestructura |
| P-03 Nombres y módulos de puertos/memoria | Decidir nombres al implementar, manteniendo límites de capas |
| P-04 Representación/rounding estado inicial | ISO 8601 UTC; minutos enteros hacia abajo |
