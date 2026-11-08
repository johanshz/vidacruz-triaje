# ADR-06 — Despliegue en Azure

- **Fecha:** 2026-11-06
- **Estado:** Aceptada

## Contexto
El enunciado pide manifests de Kubernetes (Deployment, Service, probes) para AKS o Azure
Container Apps, y una URL pública con las APIs. Presupuesto: cuenta gratis de 200 USD por 30
días. Prioridad: simplicidad.

## Decisión
- **AKS, nivel gratis** (sin costo de plano de control), **1 nodo `Standard_B2ms`** (2 vCPU, 8 GB).
- Namespace `vidacruz`: Triage (2 réplicas, para demostrar la idempotencia), Asistente (1),
  PostgreSQL (StatefulSet con disco), Kafka `apache/kafka` en modo KRaft (StatefulSet, 1 nodo),
  Redis (Deployment).
- Services `LoadBalancer` para Triage y Asistente (la URL pública); el resto `ClusterIP`.
- Probes: `/actuator/health/liveness` y `/actuator/health/readiness`.
- Secretos (`ANTHROPIC_API_KEY`, API keys, clave de PostgreSQL) en un `Secret` que crea el script
  desde variables de entorno; nunca en git.
- Imágenes públicas en GitHub Container Registry. Scripts `deploy/azure/construir-imagenes.ps1` y `deploy/azure/desplegar.ps1` con `az` y `kubectl`.
- Tras la evaluación: `deploy/azure/apagar-encender.ps1 -Accion apagar` (`az aks stop`) para no gastar.

## Opciones descartadas
| Opción | Por qué no |
|---|---|
| Azure Container Apps | No usa manifests de Kubernetes, que el enunciado pide. Queda como plan B si no hay cupo de vCPU |
| Event Hubs (Kafka), PostgreSQL Flexible Server, Azure Cache for Redis | Lo correcto en producción, pero 3 servicios más que crear y pagar; se documenta como ruta de producción |
| Bitnami para Kafka | Sus imágenes gratuitas cambiaron en 2025; la imagen oficial de Apache es suficiente |
| Azure Container Registry | Costo mensual; GHCR público es gratis |

## Consecuencias
- Un solo nodo: sin alta disponibilidad de infraestructura (aceptado para una prueba).
- Los mismos contenedores que en `docker-compose`: lo que corre local corre en Azure.
- Costo: el nodo + 2 IP públicas + discos. Cubierto por el crédito de la cuenta gratis de Azure.

## Cómo se defiende
"Kubernetes real con manifests, al menor costo; en producción movería Kafka, PostgreSQL y Redis
a servicios administrados sin cambiar el código."
