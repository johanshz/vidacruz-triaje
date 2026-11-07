# Triage

Microservicio reactivo de triaje de VidaCruz. Registra, clasifica y consulta pacientes, cambia su estado y publica eventos confirmados mediante el outbox.

## Arranque local

1. Copia `.env.example` a `.env` y cambia las claves de ejemplo por valores locales.
2. Ejecuta `docker compose up --build`.
3. El servicio queda disponible en `http://localhost:8081`.

Liveness y readiness no requieren API key:

```bash
curl -i http://localhost:8081/actuator/health/liveness
curl -i http://localhost:8081/actuator/health/readiness
```

Los errores usan `application/problem+json`, con `codigo` y `traceId`: `VALIDACION` (400), `NO_AUTORIZADO` (401), `NO_ENCONTRADO` (404), `TRANSICION_INVALIDA` (409) y `ERROR_INTERNO` (500).

## Cómo probar el camino del paciente

Define la API key de `.env` en la terminal. Los seis registros corresponden a los ejemplos Ceiba E1-E6:

```bash
export TRIAGE_API_KEY=cambia-esta-api-key-local
BASE=http://localhost:8081/triage/pacientes
curl -i -H "X-API-Key: $TRIAGE_API_KEY" -H 'Content-Type: application/json' -d '{"nombreCompleto":"Paciente E1","edad":40,"frecuenciaCardiaca":130,"presionSistolica":180,"presionDiastolica":110,"spo2":97,"temperatura":36.8,"dolor":8,"gcs":15,"quejaPrincipal":"Dolor torácico"}' "$BASE"
curl -i -H "X-API-Key: $TRIAGE_API_KEY" -H 'Content-Type: application/json' -d '{"nombreCompleto":"Paciente E2","edad":40,"frecuenciaCardiaca":95,"presionSistolica":150,"presionDiastolica":95,"spo2":91,"temperatura":37.0,"dolor":4,"gcs":15,"quejaPrincipal":"Dificultad para respirar"}' "$BASE"
curl -i -H "X-API-Key: $TRIAGE_API_KEY" -H 'Content-Type: application/json' -d '{"nombreCompleto":"Paciente E3","edad":40,"frecuenciaCardiaca":88,"presionSistolica":130,"presionDiastolica":85,"spo2":98,"temperatura":38.2,"dolor":6,"gcs":15,"quejaPrincipal":"Fiebre"}' "$BASE"
curl -i -H "X-API-Key: $TRIAGE_API_KEY" -H 'Content-Type: application/json' -d '{"nombreCompleto":"Paciente E4","edad":40,"frecuenciaCardiaca":72,"presionSistolica":120,"presionDiastolica":80,"spo2":99,"temperatura":36.5,"dolor":2,"gcs":15,"quejaPrincipal":"Herida por cortar"}' "$BASE"
curl -i -H "X-API-Key: $TRIAGE_API_KEY" -H 'Content-Type: application/json' -d '{"nombreCompleto":"Paciente E5","edad":40,"frecuenciaCardiaca":70,"presionSistolica":115,"presionDiastolica":75,"spo2":99,"temperatura":36.4,"dolor":0,"gcs":15,"quejaPrincipal":"Renovación de recetario"}' "$BASE"
curl -i -H "X-API-Key: $TRIAGE_API_KEY" -H 'Content-Type: application/json' -d '{"nombreCompleto":"Paciente E6","edad":40,"frecuenciaCardiaca":35,"presionSistolica":80,"presionDiastolica":50,"spo2":85,"temperatura":35.9,"dolor":0,"gcs":6,"quejaPrincipal":"Inconsciente"}' "$BASE"
```

Guarda el `id` devuelto para consultar y cambiar estado. El primer PATCH es válido para un paciente `REGISTRADO`; repetir una transición no permitida responde 409:

```bash
ID=<uuid-devuelto>
curl -i -H "X-API-Key: $TRIAGE_API_KEY" "$BASE/$ID"
curl -i -X PATCH -H "X-API-Key: $TRIAGE_API_KEY" -H 'Content-Type: application/json' -d '{"estado":"EN_CONSULTA"}' "$BASE/$ID/estado"
curl -i -X PATCH -H "X-API-Key: $TRIAGE_API_KEY" -H 'Content-Type: application/json' -d '{"estado":"REGISTRADO"}' "$BASE/$ID/estado"
```

Inspecciona los eventos publicados:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:19092 --topic triaje.eventos --from-beginning
```

## Configuración

`KAFKA_BOOTSTRAP_SERVERS` apunta a `kafka:19092` dentro de Compose o a `localhost:9092` al ejecutar la aplicación fuera de Docker. `VIDACRUZ_OUTBOX_INTERVALO` y `VIDACRUZ_OUTBOX_LOTE` controlan el intervalo y tamaño del lote (por defecto `PT1S` y `100`). `VIDACRUZ_OUTBOX_TIMEOUT_ENVIO` limita el tiempo de cada envío a Kafka (por defecto `PT10S`). Kafka no forma parte de readiness. Si el broker está caído al arranque, Triage inicia y conserva los eventos en PostgreSQL.
