# EcoVerde Backend & IA — Sistema de Monitoreo Fitosanitario

Ecosistema backend centralizado (Micronaut + PostgreSQL + AWS Lambda IA) para la detección temprana y control del escarabajo descortezador (*Dendroctonus spp.* y *Ips spp.*) en coníferas mexicanas, bajo la **NOM-019-SEMARNAT-2017**.

---

## Componentes del Proyecto

- **[`backend/`](backend/)**: API REST centralizada en **Java 21** y **Micronaut 4.7.4** con arquitectura limpia y soporte offline-first.
- **[`ai-service/`](ai-service/)**: Microservicio serverless en **Python (FastAPI / LiteRT)** para inferencia de alta precisión en la nube (AWS Lambda).
- **[`models/`](models/)**: Modelos entrenados (Keras / TFLite) y scripts de entrenamiento con transfer learning sobre EfficientNet-B3.
- **[`docker/`](docker/)**: Configuración de contenedores PostgreSQL 16 e inicialización de esquema.
- **[`docs/`](docs/)**: Documentación técnica, biológica y guías de API.

---

## Inicio Rápido

### 1. Iniciar Base de Datos (PostgreSQL)
```bash
docker compose up -d
```

### 2. Iniciar Servicio Backend
```bash
cd backend
./gradlew run
```
Servidor disponible en `http://localhost:8080`.

---

## Documentación Detallada

- [`AGENTS.md`](AGENTS.md): Guía de desarrollo, arquitectura y convenciones para agentes de IA y colaboradores.
- [`docs/Guia de uso de APIs del backend.md`](docs/Guia de uso de APIs del backend.md): Catálogo exhaustivo de endpoints y contratos JSON.
- [`docs/Base_Conocimiento_TT_Descortezadores.md`](docs/Base_Conocimiento_TT_Descortezadores.md): Biología de la plaga y protocolos fitosanitarios de la NOM-019-SEMARNAT-2017.
- [`docs/walkthrough.md`](docs/walkthrough.md): Recorrido conceptual de la arquitectura de la solución.
