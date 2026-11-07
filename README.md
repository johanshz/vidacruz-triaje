# Asistente VidaCruz

## Reintentos y DLT

Los errores de Redis del listener se reintentan dentro del flujo reactivo con backoff exponencial
(1, 2 y 4 segundos) y, al agotarse, se publican en `triaje.eventos.DLT`. El `DefaultErrorHandler`
atiende los fallos síncronos del listener y los errores de deserialización. En esta ruta reactiva,
el listener completa su etapa asíncrona después de publicar en la DLT, por lo que el contenedor no
recibe ese error. La DLT serializa el evento ya deserializado y no necesariamente conserva los bytes
originales del mensaje.

## Vista de urgencias

### Entorno local

1. Copia `.env.example` como `.env` y asigna una clave local a `ASISTENTE_API_KEY`.
2. Ejecuta `docker compose up --build -d`.
3. El Compose espera Redis y Kafka sanos. El servicio `topic-init` crea `triaje.eventos` con tres particiones para las pruebas locales. Esa creación pertenece al entorno de pruebas; el servicio Asistente no crea el topic de entrada. En el despliegue completo, el topic lo crea Triage.

Redis usa AOF y un volumen nombrado. Kafka corre en KRaft. Solo se publica `8082:8080`; Redis y Kafka permanecen en la red privada de Compose.

### Variables de entorno

| Variable | Requerida | Uso |
|---|---|---|
| `SERVER_PORT` | No, 8080 | Puerto del servidor |
| `REDIS_HOST` | Sí | Host Redis |
| `REDIS_PORT` | No, 6379 | Puerto Redis |
| `REDIS_PASSWORD` | No | Clave Redis; vacía en local |
| `REDIS_TIMEOUT` | No, 2s | Límite configurable de operación |
| `KAFKA_BOOTSTRAP_SERVERS` | Sí | Servidores Kafka |
| `ASISTENTE_API_KEY` | Sí | Clave de la cabecera `X-API-Key` |
| `KAFKA_GROUP_ID` | No, `asistente-urgencias` | Grupo consumidor |
| `KAFKA_CONCURRENCY` | No, 3 | Consumidores concurrentes |
| `KAFKA_MAX_POLL_RECORDS` | No, 50 | Máximo de registros por poll |
| `KAFKA_RETRY_INITIAL_MS` | No, 1000 | Espera inicial de reintento |
| `KAFKA_RETRY_MAX_MS` | No, 4000 | Espera máxima de reintento |
| `KAFKA_RETRY_MULTIPLIER` | No, 2.0 | Multiplicador de espera |

### Publicar un evento DP3 de prueba

```sh
docker compose exec kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server kafka:9092 --topic triaje.eventos \
  --property parse.key=true --property key.separator=:
```

Pega una línea como esta; la llave del mensaje debe ser igual a `datos.idPaciente`:

```text
paciente-001:{"idEvento":"evento-001","tipo":"paciente_registrado","version":1,"ocurridoEn":"2026-11-08T10:00:00Z","datos":{"idPaciente":"paciente-001","nivel":"URGENTE","estadoAnterior":null,"estadoNuevo":"REGISTRADO","escalado":false}}
```

Los tipos procesados son `paciente_registrado`, `paciente_en_consulta`, `paciente_alta` y `paciente_escalado`. El registro agrega el paciente al conjunto de su nivel; el alta lo elimina de los cinco conjuntos. Consulta y escalamiento no cambian el nivel.

### Consulta y probes

```sh
curl -H "X-API-Key: TU_CLAVE" http://localhost:8082/asistente/estado-urgencias
curl http://localhost:8082/actuator/health/liveness
curl http://localhost:8082/actuator/health/readiness
```

La respuesta de consulta trae los cinco niveles en orden fijo, conteos en cero cuando no hay pacientes, su suma en `totalActivos` y `consultadoEn` UTC. Readiness no depende de Redis ni Kafka; si Redis falla, la consulta devuelve 503 `URGENCIAS_NO_DISPONIBLE`.

Todas las respuestas incluyen `X-Trace-Id`. Los errores usan ProblemDetail: 401 `NO_AUTORIZADO`, 404 `NO_ENCONTRADO`, 405 `SOLICITUD_INVALIDA`, 503 `URGENCIAS_NO_DISPONIBLE` y 500 `ERROR_INTERNO`.

### Supuestos de esta entrega

- **S-10:** tipos desconocidos o versiones distintas de 1 se ignoran y generan log WARN; eventos mal formados se publican en `triaje.eventos.DLT` sin reintentos.
- **S-11:** readiness no depende de Redis ni Kafka; si Redis está caído, el GET responde 503 `URGENCIAS_NO_DISPONIBLE`.

## Asistente — consulta de protocolos

### Variables

| Variable | Requerida | Uso |
|---|---|---|
| `ANTHROPIC_API_KEY` | No al arrancar | Clave de Anthropic; sin ella la consulta de IA responde 503 `IA_NO_DISPONIBLE` |
| `ASISTENTE_MODELO` | No, `claude-sonnet-5-5` | Modelo de chat |
| `ASISTENTE_UMBRAL_SIMILITUD` | No, `0.75` inicial | Umbral de recuperación; requiere calibración para fijar su valor final |
| `ASISTENTE_TOP_K` | No, `3` | Máximo de fragmentos recuperados |
| `ASISTENTE_IA_TIMEOUT` | No, `10s` | Límite de la llamada al chat |
| `ASISTENTE_EMBEDDINGS_TIMEOUT` | No, `10s` | Límite de la búsqueda y generación local de embeddings |
| `ASISTENTE_IA_VENTANA` | No, `10` | Tamaño de ventana del circuit breaker |
| `ASISTENTE_IA_MINIMO_LLAMADAS` | No, `5` | Mínimo de llamadas antes de evaluar fallos |
| `ASISTENTE_IA_PORCENTAJE_FALLOS` | No, `50` | Porcentaje de fallos para abrir el circuito |
| `ASISTENTE_IA_ESPERA_ABIERTO` | No, `30s` | Espera antes de probar recuperación |
| `ASISTENTE_IA_LLAMADAS_SEMIABIERTO` | No, `2` | Llamadas de prueba en semia­bierto |

### Modelo local de embeddings

El índice usa ONNX local. Obtén `model.onnx` y `tokenizer.json` con checksum verificado mediante
`scripts/descargar-modelo.ps1` (PowerShell) o `scripts/descargar-modelo.sh` (Linux/Docker). En Docker
la imagen descarga los archivos durante la etapa de build si no existen. El cargador indexa los tres
protocolos (siete secciones) al arrancar y detiene el inicio si el corpus o el modelo no cargan.

### Consultas de ejemplo

Ejecuta cada consulta contra `http://localhost:8082/asistente/consulta` con `X-API-Key`:

```sh
curl -X POST http://localhost:8082/asistente/consulta \
  -H "X-API-Key: TU_CLAVE" -H 'Content-Type: application/json' \
  -d '{"pregunta":"¿Cómo se maneja una crisis hipertensiva?"}'

curl -X POST http://localhost:8082/asistente/consulta \
  -H "X-API-Key: TU_CLAVE" -H 'Content-Type: application/json' \
  -d '{"pregunta":"¿Cuál es el primer antibiótico en sepsis?"}'

curl -X POST http://localhost:8082/asistente/consulta \
  -H "X-API-Key: TU_CLAVE" -H 'Content-Type: application/json' \
  -d '{"pregunta":"¿Cuál es la primera línea en diabetes tipo 2?"}'
```

La primera consulta debe recuperar `PROTO-001`, la segunda `PROTO-002`; la tercera debe indicar que
no existe protocolo. Sin coincidencia sobre el umbral o si fallan las barreras de fuentes/cifras, se
devuelve la frase fija de no cobertura con `fuentes: []` y sin llamar al LLM cuando no hay contexto.
Las cinco barreras validan la entrada, la cobertura del índice, las fuentes, las citas respecto de
los fragmentos recuperados y las cifras de la respuesta. Una caída o circuito abierto del proveedor
devuelve 503 `IA_NO_DISPONIBLE`.

La pregunta debe contener de 3 a 500 caracteres tras recortarla (S-15). No cobertura o rechazo de
una barrera devuelve 200 con el aviso fijo (S-16); las cifras se validan contra fragmentos, no contra
la pregunta (S-17). La clave de Anthropic no es necesaria durante el arranque (S-18). El error 503
usa `codigo: IA_NO_DISPONIBLE` (S-19). El umbral `0.75` es provisional (S-08), pendiente de
calibración con las tres consultas y el modelo ONNX real. **PE pendiente de Johan:** ejecutar también la prueba de integración CA-22 con BlockHound cuando estén disponibles `model.onnx` y `tokenizer.json`; hasta entonces esa prueba se omite explícitamente y el índice no se declara calibrado.

### Pendientes de ejecución con credenciales/modelo reales

- **PE-01 — Johan:** ejecutar la comparación Sonnet 5.5/Haiku 5.5 con las tres consultas y registrar
  respuesta, fuentes, resultado de las cinco barreras, latencia, costo estimado y decisión en
  `desarrollo-ia/asistente/comparacion-modelos.md`. Sonnet permanece como modelo por defecto hasta
  completar la comparación.
- **PE-02 — Johan:** calibrar el umbral con el modelo ONNX real y las consultas de `PROTO-001`,
  `PROTO-002` y sin protocolo; registrar el resultado antes de fijar el umbral final.
