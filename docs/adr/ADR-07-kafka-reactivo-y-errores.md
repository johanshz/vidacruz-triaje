# ADR-07 — Kafka reactivo, contrapresión y eventos que fallan

- **Fecha:** 2026-11-06
- **Estado:** Aceptada

## Contexto
Se evalúa "integración reactiva (Reactor, Kafka, backpressure)" y "manejo de errores". La
librería clásica, Reactor Kafka, fue descontinuada por el equipo de Spring en mayo de 2025
(1.3 es la última versión menor; la plantilla reactiva de Spring Kafka quedó deprecada).

## Decisión
1. **Spring for Apache Kafka** (no Reactor Kafka):
   - Producción: `Mono.fromFuture(kafkaTemplate.send(...))`, sin bloquear.
   - Consumo: `@KafkaListener` que **devuelve `Mono`** (retorno asíncrono, Spring Kafka 3.2+):
     el offset se confirma cuando el `Mono` termina.
2. **Contrapresión:** el consumidor de Kafka es de tirar (pull). `max.poll.records` y la
   concurrencia del contenedor limitan cuántos eventos entran; el siguiente lote no se pide
   hasta procesar el actual. En SSE, el `Sinks.Many` con búfer acotado protege de clientes lentos.
3. **Eventos que fallan:** `DefaultErrorHandler` con 3 reintentos y espera creciente; si sigue
   fallando, `DeadLetterPublishingRecoverer` lo envía a `triaje.eventos.DLT` y la partición sigue.
   Un evento con JSON inválido va directo a la DLT (no se reintenta). Log de error y métrica.

## Opciones descartadas
| Opción | Por qué no |
|---|---|
| Reactor Kafka | Descontinuado: entregar una prueba nueva sobre una librería sin mantenimiento resta |
| Spring Cloud Stream reactivo | Su binder de Reactor Kafka está marcado para retiro |
| Reintentar sin fin | Un evento dañado trancaría la partición para todos los pacientes |
| Descartar con log | El evento se pierde en silencio; choca con la prioridad de trazabilidad |

## Consecuencias
- Nada del procesamiento corre en el event loop de Netty: el listener tiene sus propios hilos y
  la cadena hacia Redis es reactiva.
- La DLT queda para revisión manual; reintentar es seguro porque los consumidores son idempotentes.

Fuente: https://spring.io/blog/2025/05/20/reactor-kafka-discontinued

## Cómo se defiende
"Reactor Kafka murió en 2025; uso Spring Kafka con listeners que devuelven Mono, la contrapresión
la da el modelo pull de Kafka, y un evento dañado va a la DLT sin frenar a los demás."
