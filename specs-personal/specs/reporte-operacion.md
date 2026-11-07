# reporte-operacion

### Requirement: Reporte de operación horario calculado en PostgreSQL
`GET /triage/reportes/operacion` requiere `desde` y `hasta` ISO 8601 (`Z` u offset), `desde < hasta` y rango máximo de 31 días. Devuelve 200 JSON con `desde`, `hasta`, `zonaHoraria` y `horas` ordenadas que tengan registros. Cada elemento incluye `hora` con offset de la zona configurada, `totalRegistrados`, `horas[].acumuladoRegistrados` (suma de `totalRegistrados` de las horas con registros hasta esa hora inclusive; campo propio del Triage), `porNivel` con las cinco claves (`CRITICO`, `URGENTE`, `URGENTE_2`, `NO_URGENTE`, `NO_URGENTE_2`) siempre presentes y 0 si no hay registros, y `esperaPromedioMinutos` y `esperaP95Minutos` con 1 decimal (`ROUND(...::numeric,1)`) o null. PostgreSQL agrupa por hora local y calcula agregados, percentil y acumulado mediante `SUM(COUNT(*)) OVER (ORDER BY hora)`. Espera y p95 consideran solo pacientes con primera consulta. No itera registros en memoria; rango vacío produce lista vacía. Endpoint protegido con `X-API-Key`, sin clave por query.

**Origen en el RFC:** RN-15, RN-16, RN-17, RN-18

#### Scenario: Reporte agrupa y calcula esperas
**Origen en el RFC:** CA-21, CA-26
- **WHEN** se consultan varias horas con pacientes de distintos niveles y esperas conocidas
- **THEN** retorna horas ordenadas, total, cinco niveles, promedio y p95 con un decimal conforme a `percentile_cont(0.95)`

#### Scenario: Acumulado y horas sin espera
**Origen en el RFC:** CA-22, CA-23
- **WHEN** existen horas con registros y una hora sin pacientes que entraron a consulta
- **THEN** `horas[].acumuladoRegistrados` suma `totalRegistrados` de las horas con registros hasta esa hora inclusive; no emite horas sin registros, y entrega promedio/p95 null donde no hay espera

#### Scenario: Rango vacío y bordes horarios
**Origen en el RFC:** CA-24, CA-25
- **WHEN** se consulta un rango vacío o con registros en sus límites y cerca del cambio horario
- **THEN** responde `horas: []` si está vacío; incluye `desde`, excluye `hasta` y presenta la hora con offset de la zona configurada

#### Scenario: Parámetros inválidos
**Origen en el RFC:** CA-27
- **WHEN** falta o no es ISO `desde`/`hasta`, `desde >= hasta` o el rango supera 31 días
- **THEN** responde 400 `VALIDACION` con campo `desde` si falta o no es ISO 8601; con campo `hasta` si falta o no es ISO 8601, si `desde >= hasta`, o si el rango supera 31 días; no consulta la base

#### Scenario: Reporte requiere API key
**Origen en el RFC:** CA-28
- **WHEN** se llama sin clave válida
- **THEN** responde 401 `NO_AUTORIZADO`
