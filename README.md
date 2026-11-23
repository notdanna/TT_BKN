# EcoVerde Backend & IA — Sistema de Monitoreo Fitosanitario

[![Backend & AI CI](https://github.com/sheets-tt/bkn-sheet/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/sheets-tt/bkn-sheet/actions/workflows/backend-ci.yml)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Micronaut](https://img.shields.io/badge/Micronaut-4.7.4-000000?logo=micronaut&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-LiteRT-009688?logo=fastapi&logoColor=white)

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
# Obligatorio: secreto HS256 de los JWT (>= 32 bytes). No hay valor por defecto.
export JWT_GENERATOR_SIGNATURE_SECRET="$(openssl rand -base64 48)"
./gradlew run
```
Servidor disponible en `http://localhost:8080`.

---

## Documentación Detallada

- [`AGENTS.md`](AGENTS.md): Guía de desarrollo, arquitectura y convenciones para agentes de IA y colaboradores.
- [`docs/Guia de uso de APIs del backend.md`](docs/Guia de uso de APIs del backend.md): Catálogo exhaustivo de endpoints y contratos JSON.
- [`docs/Base_Conocimiento_TT_Descortezadores.md`](docs/Base_Conocimiento_TT_Descortezadores.md): Biología de la plaga y protocolos fitosanitarios de la NOM-019-SEMARNAT-2017.
- [`docs/walkthrough.md`](docs/walkthrough.md): Recorrido conceptual de la arquitectura de la solución.

---

## Seguridad (noviembre 2026)

- **Secreto del JWT:** `JWT_GENERATOR_SIGNATURE_SECRET` es obligatorio y no tiene valor por defecto.
  El que estaba en `application.yml` quedó publicado en el historial de git: `ValidadorSecretoJwt`
  impide arrancar con él o con uno de menos de 32 bytes. En la Lambda hay que definirlo **antes** de
  desplegar esta versión (si no, no arranca). Cambiarlo invalida las sesiones abiertas.
- **Borrados:** `DELETE /api/v1/arboles` y `DELETE /api/v1/zonas-follaje` (vaciar todo) son solo de
  `Administrador`; borrar un árbol o una zona es del dueño (`id_cuenta` del token) o de un `Administrador`
  (403 si no). La app móvil llama al borrado masivo al limpiar su caché, dentro de `runCatching`, así que
  para un experto el 403 no rompe esa limpieza local.
- **Esquema:** `analisis.id_arbol`/`id_zona` pasan a `ON DELETE CASCADE`
  (`docker/postgres/migraciones/2026-11-09_analisis_on_delete_cascade.sql` para la BD existente).

## Autor de cada análisis (noviembre 2026)

- **Esquema:** `analisis.id_cuenta` (nullable, `ON DELETE SET NULL`) guarda la cuenta que subió cada
  análisis; la sincronización la toma del token. Para la BD existente:
  `docker/postgres/migraciones/2026-11-17_analisis_id_cuenta.sql`, **antes** de desplegar esta versión.
  Sin la columna no solo falla la sincronización (`/sync` responde 200 con `totalSincronizados: 0` y
  `column id_cuenta does not exist` en `errores`): **todas** las lecturas de análisis seleccionan
  `id_cuenta` y responden 500 (`GET /api/v1/analisis`, `GET /api/v1/arboles/{id}/analisis`, historial,
  estadísticas y reportes). Los análisis anteriores quedan con autor desconocido (`NULL`).
- **Rollback**, en este orden: 1) volver TT_BKN a la versión anterior; 2) `DROP INDEX IF EXISTS
  idx_analisis_cuenta; ALTER TABLE analisis DROP COLUMN IF EXISTS id_cuenta;`; 3) reiniciar o redesplegar
  b-sheet, que detecta la columna una vez por arranque y, si la quitan después, responde 500 en los
  análisis hasta su siguiente arranque en frío.
- **b-sheet** funciona con o sin la columna (sin ella, `autor = null`); el autor aparece en la web a partir
  del siguiente arranque en frío o redespliegue de su Lambda después de aplicar la migración.
- **API:** `AnalisisDetalleDTO` añade `idCuenta` (nullable); los campos existentes no cambian.
