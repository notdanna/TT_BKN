# AGENTS.md — Guía para Agentes y Desarrolladores (EcoVerde Backend & IA)

> **EcoVerde Backend** es el ecosistema de servicios de backend, base de datos relacional y servicios de inferencia de IA en la nube para el monitoreo fitosanitario y detección de escarabajos descortezadores (*Dendroctonus spp.* y *Ips spp.*), alineado a la **NOM-019-SEMARNAT-2017**.

---

## 1. Visión General del Repositorio

Este repositorio consolida:
1. **API REST Centralizada (`backend/`)**: Desarrollada en **Java 21** con **Micronaut 4.7.4**, Clean Architecture, persistencia en **PostgreSQL 16** y autenticación segura **JWT + BCrypt**.
2. **Servicio de Inferencia Cloud (`ai-service/`)**: Microservicio en **Python (FastAPI / LiteRT)** empaquetado para AWS Lambda / Fargate con el modelo de alta precisión **EfficientNet-B3** (Full Precision).
3. **Canal de Entrenamiento (`models/`)**: Scripts de transfer learning en dos fases con aumento de datos y evaluación basada en Recall/Precision orientada a control fitosanitario.
4. **Infraestructura Local (`docker/` y `docker-compose.yml`)**: Contenedores para PostgreSQL 16 y servicios de desarrollo.

---

## 2. Pila Tecnológica (Tech Stack)

| Componente | Tecnologías |
|---|---|
| **Backend REST** | Java 21, Micronaut 4.7.4, Gradle, HikariCP, JDBC / JDBI, JWT (JJWT), BCrypt |
| **Base de Datos** | PostgreSQL 16 con PostGIS/extensiones espaciales, Docker Compose |
| **Servicio de IA** | Python 3.11+, LiteRT (AI Edge), NumPy, Pillow, FastAPI / AWS Lambda Proxy |
| **Entrenamiento** | TensorFlow 2.16+, Keras, EfficientNet-B3, Scikit-Learn |
| **Almacenamiento** | Amazon S3 / Almacenamiento local en disco para evidencias fotográficas |

---

## 3. Estructura de Directorios

```text
TT_BKN/
├── backend/                                   # Servicio principal Micronaut REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/descortezadores/backend/
│   │   │   │   ├── Application.java           # Clase de entrada Micronaut
│   │   │   │   ├── controllers/               # Controladores REST (/api/v1/...)
│   │   │   │   ├── services/                  # Lógica de negocio y casos de uso
│   │   │   │   ├── repositories/              # Acceso a datos PostgreSQL
│   │   │   │   ├── models/                    # Entidades y DTOs
│   │   │   │   └── security/                  # Proveedores JWT y filtros de autorización
│   │   │   └── resources/
│   │   │       ├── application.yml            # Configuración de entornos y datasource
│   │   │       └── db/migration/              # Scripts SQL de evolución de esquema
│   │   └── test/                              # Pruebas unitarias e integración de API
│   ├── build.gradle                           # Dependencias y plugins Gradle de Micronaut
│   └── README.md                              # Guía interna del módulo backend
├── ai-service/                                # Servicio de inferencia Cloud para AWS
│   ├── app.py                                 # Manejador Lambda y endpoint FastAPI
│   ├── classifier_full.tflite                 # Modelo EfficientNet-B3 (Float32 full-precision)
│   ├── Dockerfile                             # Imagen base Linux para Lambda/Container
│   └── requirements.txt                       # Dependencias mínimas de inferencia
├── models/                                    # Entrenamiento y exportación de modelos
│   ├── entrenamiento_efficientnetb3.py        # Pipeline de 2 fases (congelado + fine-tuning)
│   ├── classifier.keras                       # Artefacto nativo Keras
│   └── light_classifier.tflite                # Modelo cuantizado para la app móvil
├── docker/                                    # Recursos de despliegue local
│   └── postgres/
│       └── init-schema.sql                    # DDL de inicialización (7 tablas)
├── docs/                                      # Documentación técnica y botánica
│   ├── Base_Conocimiento_TT_Descortezadores.md# Biología, especies y NOM-019-SEMARNAT-2017
│   ├── Guia de uso de APIs del backend.md    # Especificación completa de contratos REST
│   └── walkthrough.md                         # Recorrido de arquitectura del sistema
├── docker-compose.yml                         # Orquestación de PostgreSQL para desarrollo
├── AGENTS.md                                  # Este archivo de instrucciones para agentes
└── README.md                                  # Guía general de inicio rápido
```

---

## 4. Comandos de Compilación, Ejecución y Pruebas

### 4.1 Base de Datos (Docker)
```bash
# Iniciar contenedor PostgreSQL
docker compose up -d

# Detener contenedor
docker compose down

# Ver logs de base de datos
docker compose logs -f db
```

### 4.2 Servicio Backend (Micronaut)
```bash
cd backend

# Compilar proyecto y verificar dependencias
./gradlew build -x test

# Ejecutar pruebas unitarias e integración
./gradlew test

# Ejecutar servidor de desarrollo (http://localhost:8080)
./gradlew run
```

### 4.3 Servicio de IA (Python)
```bash
cd ai-service

# Crear entorno virtual e instalar dependencias
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Ejecutar localmente con uvicorn / prueba unitaria
python3 app.py
```

---

## 5. Endpoints Clave y Contratos de la API

| Método | Endpoint | Autenticación | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/health` | Pública | Verificación de salud del sistema (`UP`). |
| `POST` | `/login` | Pública | Login de biólogos/admins (`correo`, `password`). Emite JWT. |
| `POST` | `/api/v1/analisis/clasificar` | Pública / JWT | Inferencia de alta precisión en la nube enviando fotografía en multipart. |
| `POST` | `/api/v1/sync/detecciones` | JWT Requerido | Recepción en bloque (batch) de registros creados offline en campo. |
| `GET` | `/api/v1/arboles` | JWT Requerido | Catálogo de árboles con coordenadas GPS y estado fitosanitario. |
| `POST` | `/api/v1/arboles` | JWT Requerido | Alta individual de nuevo árbol con coordenadas de captura. |
| `GET` | `/api/v1/zonas-follaje` | JWT Requerido | Zonas de monitoreo de dosel / tomas aéreas registradas. |
| `GET` | `/api/v1/estadisticas/focos` | JWT Requerido | Agrupación espacial automática de focos de infestación activos. |
| `GET` | `/api/v1/estadisticas/dashboard`| JWT Requerido | Métricas para el panel web y agregados epidemiológicos. |
| `POST`| `/api/v1/evidencias/upload` | JWT Requerido | Carga multipart de fotografías hacia el almacenamiento seguro (S3). |

---

## 6. Reglas de Negocio y Protocolos Forestales

1. **Cumplimiento de la NOM-019-SEMARNAT-2017**:
   - Todo diagnóstico con resultado `INFESTADO` genera automáticamente una zona de alerta de **100 metros a la redonda** para inspección obligatoria de hospederos adyacentes.
   - El árbol queda registrado con fecha, hora exacta, coordenadas GPS con 5 decimales de precisión y biólogo responsable.
2. **Offline-First & Sincronización Segura**:
   - La API debe aceptar cargas idempotentes mediante UUIDs generados en la app móvil (`idArbol`, `idZona`, `idAnalisis`).
   - El endpoint `/api/v1/sync/detecciones` maneja tanto muestras de corteza como evaluaciones cromáticas de dosel.
3. **Métricas de Clasificación en el Modelo**:
   - El modelo prioriza maximizar el **Recall** (sensibilidad) sobre la clase `INFESTADO`. En fitosanidad forestal, un Falso Negativo (árbol infestado no detectado) es biológicamente catastrófico, pues propaga el foco en todo el bosque.

---

## 7. Instrucciones para Agentes al Modificar Código

1. **Persistencia y Compatibilidad de Esquema**: Si modificas tablas o entidades en `backend/`, mantén la compatibilidad hacia atrás para no romper clientes móviles existentes.
2. **Validación de Tipos y Seguridad**: Todo endpoint protegido debe validar el token Bearer en el header `Authorization`.
3. **Preservar Documentación**: No elimines comentarios explicativos en scripts de entrenamiento ni en clases Java de arquitectura limpia.
4. **Pruebas Automatizadas**: Corre `./gradlew test` en `backend/` antes de concluir cualquier cambio en el backend.
5. **Consultas de Referencia**: Para detalles específicos sobre esquemas JSON de respuesta, consulta [`docs/Guia de uso de APIs del backend.md`](docs/Guia de uso de APIs del backend.md).
