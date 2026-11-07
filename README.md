# Triage

Microservicio reactivo del sistema VidaCruz. Esta entrega deja preparada la infraestructura transversal; los endpoints de negocio se agregan en las siguientes filas del RFC.

## Arranque local

1. Copia `.env.example` a `.env` y cambia las claves de ejemplo por valores locales.
2. Ejecuta `docker compose up --build`.
3. El servicio queda disponible en `http://localhost:8081`.

Liveness y readiness no requieren API key:

```bash
curl -i http://localhost:8081/actuator/health/liveness
curl -i http://localhost:8081/actuator/health/readiness
```

`/actuator/info` y `/actuator/prometheus` requieren `X-API-Key`:

```bash
curl -i http://localhost:8081/actuator/info
curl -i -H "X-API-Key: $TRIAGE_API_KEY" http://localhost:8081/actuator/info
```

Los errores usan `application/problem+json`, con `codigo` y `traceId`. Los códigos contemplados son `VALIDACION` (400), `NO_AUTORIZADO` (401), `NO_ENCONTRADO` (404), `TRANSICION_INVALIDA` (409), `SOLICITUD_INVALIDA` (otros 4xx) y `ERROR_INTERNO` (500). Los errores HTTP genéricos usan un detalle en español; los 500 exponen solo un mensaje genérico.

## Supuestos

- **S-02:** FC, presiones, SpO2, dolor, GCS y edad son enteros; un decimal en un campo entero produce 400.
- **S-09:** la queja principal admite máximo 500 caracteres; la validación de negocio corresponde a la fila 2.

Kafka y Redis se levantan en Compose, pero el Triage no los utiliza en esta entrega.
