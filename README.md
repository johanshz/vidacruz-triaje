# Asistente VidaCruz

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
