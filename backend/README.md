# Backend — Sistema Móvil de Detección de Insectos Descortezadores (TT 2026-B161)

API REST centralizada desarrollada en **Java 21** y **Micronaut 4.7.4** con **Clean Architecture**, persistencia en **PostgreSQL 16** y autenticación **JWT con BCrypt**. Diseñada bajo el enfoque *Offline-First* para sincronización con aplicación móvil Android y plataforma web.

---

## Requisitos y Ejecución Rápida

### 1. Base de Datos (PostgreSQL en Docker)
Desde la raíz del proyecto:
```bash
docker compose up -d
```
* **Host:** `localhost:5432` | **BD:** `descortezadores_db` | **Usuario:** `postgres` | **Password:** `postgres`
* Esquema relacional de 7 tablas inicializado automáticamente vía `docker/postgres/init-schema.sql`.

### 2. Ejecutar Servidor Backend
```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
./gradlew run
```
Servidor disponible en `http://localhost:8080`.

---

## Resumen de Endpoints Principales

* `GET /api/v1/health`: Estado del servicio (`UP`).
* `POST /login`: Autenticación y emisión de JWT (`experto@chapultepec.gob.mx` o `admin@chapultepec.gob.mx` / `Password123!`).
* `POST /sync` / `POST /api/v1/sync/detecciones`: Sincronización masiva offline (corteza + follaje, Base64 + metadatos + análisis cromático).
* `GET /api/v1/arboles`: Consulta del catálogo de árboles.
* `POST /api/v1/arboles`: Registro individual de árboles.
* `PUT /api/v1/arboles/{id}`: Actualización de datos y observaciones de un árbol.
* `GET /api/v1/arboles/{id}/analisis`: Historial de diagnósticos y capturas de un árbol.
* `GET /api/v1/analisis`: Historial general de capturas y análisis con datos cromáticos.
* `GET /api/v1/analisis/{id}`: Detalle específico de un análisis.
* `GET /api/v1/analisis/jornada`: Capturas realizadas durante la jornada en curso.
* `GET /api/v1/zonas-follaje`: Zonas de monitoreo aéreo (follaje/dron).
* `GET /api/v1/estadisticas/dashboard`: Métricas agregadas para panel web (D3.js).
* `GET /api/v1/estadisticas/focos`: Detección automática de focos de infestación activos (NOM-019-SEMARNAT-2017).
* `GET /api/v1/evidencias/{archivo}`: Visualización de fotografías de evidencia.
* `/api/v1/admin/*`: Módulo administrativo de gestión de cuentas y dispositivos (CU-A00).
