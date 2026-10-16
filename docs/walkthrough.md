# Guía Maestra y Walkthrough del Backend — Trabajo Terminal 2026-B161 (ESCOM - IPN)

> **Proyecto:** *Prototipo de sistema móvil de detección de insectos descortezadores en árboles mediante visión por computadora. Caso de estudio: Bosque de Chapultepec.*  
> **Repositorio GitHub:** [https://github.com/notdanna/TT_BKN.git](https://github.com/notdanna/TT_BKN.git)  
> **Ubicación local:** `/Users/dam/Documents/TTMobile`

---

## 1. Stack Tecnológico y Configuración de Entorno

| Componente | Tecnología | Detalle de Implementación |
|---|---|---|
| **Lenguaje** | **Java 21 (LTS)** | Aprovisionado de forma aislada vía Gradle Toolchain (`foojay-resolver`) en `~/.gradle/jdks/`. |
| **Framework Backend** | **Micronaut 4.7.4** | Inyección de dependencias AoT (Ahead-of-Time), baja latencia y preparado para despliegue en AWS Lambda. |
| **Seguridad** | **Micronaut Security JWT** | Autenticación Bearer con tokens JWT propios y hashing de contraseñas con **BCrypt** (`org.mindrot:jbcrypt`). |
| **Base de Datos** | **PostgreSQL 16** | Ejecutándose en contenedor Docker / OrbStack (`tt_postgres`) con volumen persistente. |
| **ORM / Acceso a Datos**| **Micronaut Data JDBC** | Mapeo objeto-relacional optimizado para dialecto PostgreSQL. |
| **Gestor de Construcción**| **Gradle 8.12** | Con Gradle Wrapper (`./gradlew`) incluido en el proyecto. |

---

## 2. Cómo Levantar y Ejecutar el Proyecto

### Paso 1: Levantar la Base de Datos (PostgreSQL)
Desde la raíz (`/Users/dam/Documents/TTMobile`):
```bash
docker compose up -d
```
* **Host:** `localhost` | **Puerto:** `5432`  
* **Base de datos:** `descortezadores_db` | **Usuario:** `postgres` | **Password:** `postgres`  
* El script [`docker/postgres/init-schema.sql`](file:///Users/dam/Documents/TTMobile/docker/postgres/init-schema.sql) se ejecuta automáticamente al crearse el volumen y siembra las 7 tablas oficiales y los usuarios iniciales.

### Paso 2: Compilar y Ejecutar el Backend
```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
./gradlew run
```
* El servidor iniciará en: `http://localhost:8080`.

---

## 3. Arquitectura del Código (Clean Architecture — §8.2.6)

El código en `backend/src/main/java/com/descortezadores/` está dividido estrictamente en 4 capas concéntricas:

```
com.descortezadores/
├── domain/
│   ├── entities/                  # Entidades de negocio mapeadas a PostgreSQL
│   │   ├── Cuenta.java
│   │   ├── Dispositivo.java
│   │   ├── CuentaDispositivo.java
│   │   ├── Arbol.java
│   │   ├── ZonaFollaje.java
│   │   ├── Analisis.java
│   │   └── AnalisisCromatico.java
│   └── rules/                     # Reglas de negocio e invariantes
│       ├── MensajesSistema.java   # Catálogo oficial de errores MSG1 a MSG35 (Tabla 6)
│       └── ReglasNegocioImagen.java # Validaciones RN-07 (formatos), RN-08 (10MB), RN-09 (300x300 px)
├── usecases/                      # Lógica de aplicación independiente
│   ├── sync/
│   │   └── SyncDeteccionUseCase.java      # Flujo de sincronización móvil -> nube (§8.2.5) (corteza + follaje)
│   ├── detection/
│   │   └── RegistrarDeteccionUseCase.java # Creación y registro de árboles
│   ├── history/
│   │   └── ConsultarHistorialUseCase.java # Consultas de árboles, detecciones y zonas
│   ├── stats/
│   │   └── ConsultarEstadisticasUseCase.java # Dashboard D3.js y Focos NOM-019
│   └── admin/
│       ├── GestionarCuentasUseCase.java    # Casos de uso CU-A01 a CU-A05
│       └── GestionarDispositivosUseCase.java # Casos de uso CU-A06 a CU-A09
├── adapters/
│   ├── dto/                       # Data Transfer Objects (Records serializables)
│   │   ├── ArbolDTO.java
│   │   ├── SyncItemDTO.java
│   │   ├── SyncBatchResponse.java
│   │   ├── UsuarioDTO.java
│   │   ├── DispositivoDTO.java
│   │   ├── ErrorResponseDTO.java
│   │   ├── AnalisisDetalleDTO.java
│   │   ├── AnalisisCromaticoDTO.java
│   │   ├── ZonaFollajeDTO.java
│   │   ├── DashboardEstadisticasDTO.java
│   │   └── FocoInfestacionDTO.java
│   └── controllers/               # Controladores REST HTTP
│       ├── HealthController.java        # GET /api/v1/health
│       ├── SyncController.java          # POST /sync y POST /api/v1/sync/detecciones
│       ├── ArbolController.java         # GET, POST, PUT /api/v1/arboles
│       ├── AnalisisController.java      # GET /api/v1/analisis (historial y jornada)
│       ├── ZonaFollajeController.java   # GET /api/v1/zonas-follaje
│       ├── EstadisticasController.java  # GET /api/v1/estadisticas/dashboard y /focos
│       ├── AdminController.java         # Endpoints administrativos CU-A00
│       ├── EvidenciaController.java     # GET y POST /api/v1/evidencias
│       └── ModeloController.java        # GET /api/v1/modelos/latest y /descargar (PT-15, PI-07, PI-08)
└── infrastructure/
    ├── storage/
    │   ├── AlmacenamientoService.java      # Puerto de abstracción (Clean Architecture)
    │   └── LocalAlmacenamientoService.java # Almacenamiento local en disco (desarrollo/staging)
    ├── aws/
    │   ├── AwsS3Properties.java            # Mapeo de propiedades 'aws.s3' (application.yml)
    │   ├── S3AlmacenamientoService.java    # Almacenamiento en Amazon S3 (activo con aws.s3.enabled=true)
    │   └── LambdaHandler.java              # Handler serverless para AWS Lambda con API Gateway Proxy
    ├── persistence/                        # Interfaces Micronaut Data JDBC
    │   ├── CuentaRepository.java
    │   ├── DispositivoRepository.java
    │   ├── CuentaDispositivoRepository.java
    │   ├── ArbolRepository.java
    │   ├── ZonaFollajeRepository.java
    │   ├── AnalisisRepository.java
    │   └── AnalisisCromaticoRepository.java
    └── security/
        └── DatabaseAuthenticationProvider.java # Validación BCrypt de credenciales y roles
```

---

## 4. Catálogo Completo de Endpoints REST

### A. Autenticación y Verificación de Salud

#### 1. Verificación de Salud (Público)
* **`GET /api/v1/health`**
* **Respuesta (200 OK):**
  ```json
  {
    "status": "UP",
    "service": "backend-descortezadores",
    "version": "1.0.0",
    "timestamp": "2026-09-18T17:34:50Z"
  }
  ```

#### 2. Inicio de Sesión (CU-G01 / RNF-06) (Público)
* **`POST /login`**
* **Headers:** `Content-Type: application/json`
* **Body:**
  ```json
  {
    "username": "experto@chapultepec.gob.mx",
    "password": "Password123!"
  }
  ```
  *(También disponible cuenta admin: `admin@chapultepec.gob.mx` / `Password123!`)*
* **Respuesta (200 OK):**
  ```json
  {
    "access_token": "eyJhbGciOiJIUzI1NiJ9...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "username": "experto@chapultepec.gob.mx",
    "roles": ["Experto de campo"]
  }
  ```

#### 3. Recuperar Contraseña (CU-G02 / RN-04 / RN-05 / RN-06) (Público)
* **`POST /api/v1/auth/recuperar-password`**
* **Body:** `{"correo": "experto@chapultepec.gob.mx"}`
* **Reglas:** Vigencia de 24 horas (`RN-04`), un solo uso (`RN-05`), máximo 3 solicitudes por usuario en 30 días naturales (`RN-06`).
* **Respuesta (200 OK):**
  ```json
  {
    "mensaje": "Se ha enviado el enlace o código de recuperación a su correo electrónico.",
    "codigoRecuperacion": "B8E4F1A2",
    "vigenciaHoras": 24
  }
  ```

#### 4. Restablecer Contraseña (CU-G03 / MSG7–MSG10) (Público)
* **`POST /api/v1/auth/restablecer-password`**
* **Body:**
  ```json
  {
    "token": "B8E4F1A2",
    "nuevaContrasena": "NuevaClave2026#",
    "confirmacionContrasena": "NuevaClave2026#"
  }
  ```
* **Reglas:** Valida código expirado (`MSG8`), contraseñas coincidentes (`MSG9`), requisitos de seguridad (`MSG10`) y actualiza el hash BCrypt invalidando el código (`RN-05`).

#### 5. Cerrar Sesión (CU-G04 / MSG11)
* **`POST /api/v1/auth/logout`**
* **Seguridad:** Requiere Bearer Token.

#### 6. Consultar Perfil de Usuario (CU-W09 / CU-M11)
* **`GET /api/v1/auth/perfil`**
* **Seguridad:** Requiere Bearer Token.
* **Respuesta (200 OK):** Datos de la cuenta (`idCuenta`, `correo`, `nombreCompleto`, `rol`, `estadoCuenta`, `fechaCreacion`).

---

### B. Sincronización Móvil Offline-First (§8.2.5 y PI-01)

#### 3. Sincronización por Lote de Detecciones
* **`POST /sync`** *(o `POST /api/v1/sync/detecciones`)*
* **Seguridad:** Requiere header `Authorization: Bearer <TOKEN>`
* **Body:** Lista de objetos `SyncItemDTO`:
  ```json
  [
    {
      "idAnalisisLocal": "c0000000-0000-0000-0000-000000000001",
      "idArbol": null,
      "nombreArbolNuevo": "Fresno-Seccion1-01",
      "especieArbolNuevo": "Fraxinus uhdei",
      "latitud": 19.4210,
      "longitud": -99.1820,
      "fechaCaptura": "2026-09-18T11:30:00Z",
      "tipoImagen": "corteza",
      "resultadoClasificacion": "infestado",
      "confianza": 0.96,
      "observaciones": "Presencia de galerías y grumos de resina",
      "pctIncierto": 0.05,
      "pctDanado": 0.70,
      "pctRegular": 0.15,
      "pctSano": 0.10,
      "imagenBase64": "<string_base64_jpeg_minimo_300x300px>",
      "idDispositivo": "2b799a40-0dcb-4f9a-916f-260252b72f61"
    }
  ]
  ```
* **Comportamiento:**
  1. Si `idArbol` es nulo pero `nombreArbolNuevo` viene presente, crea automáticamente el árbol en la tabla `arbol` (cumpliendo RN-37 y RN-38).
  2. Valida la imagen contra **RN-08** ($\le 10\text{ MB}$) y **RN-09** (resolución mínima $300\times300\text{ px}$).
  3. Guarda la imagen en `uploads/evidencias/{uuid}.jpg` y genera la URL permanente `/api/v1/evidencias/{uuid}.jpg`.
  4. Inserta el registro en `analisis` y `analisis_cromatico`.
* **Respuesta (200 OK):**
  ```json
  {
    "totalRecibidos": 1,
    "totalSincronizados": 1,
    "idsSincronizados": ["c0000000-0000-0000-0000-000000000001"],
    "errores": []
  }
  ```

---

### C. Catálogo de Árboles

#### 4. Listar Árboles Registrados
* **`GET /api/v1/arboles`**
* **Seguridad:** Requiere Bearer Token
* **Respuesta (200 OK):** Lista de árboles ordenados descendentemente por fecha.

#### 5. Registrar un Árbol Manualmente
* **`POST /api/v1/arboles`**
* **Seguridad:** Requiere Bearer Token
* **Body:**
  ```json
  {
    "nombreEspecifico": "Cedro-005",
    "especie": "Cupressus lusitanica",
    "latitud": 19.4205,
    "longitud": -99.1825,
    "descripcion": "Observado en Sección II"
  }
  ```
* **Respuesta (201 Created):** Objeto del árbol con `idArbol` generado.

#### 6. Actualizar Datos de un Árbol (RF-23 / RN-41)
* **`PUT /api/v1/arboles/{id}`**
* **Seguridad:** Requiere Bearer Token
* **Body:** Objeto `ArbolDTO` con datos actualizados (nombre, especie, observaciones, coordenadas).

#### 7. Consultar Historial de Análisis de un Árbol (RF-10 / RF-20 / CU-W06)
* **`GET /api/v1/arboles/{id}/analisis`**
* **Seguridad:** Requiere Bearer Token
* **Respuesta (200 OK):** Lista de `AnalisisDetalleDTO` con evidencia, estado, coordenadas y datos cromáticos.

---

### D. Consulta de Análisis y Jornada

#### 8. Listar Todos los Análisis Sincronizados (RF-20 / CU-W06)
* **`GET /api/v1/analisis`**
* **Seguridad:** Requiere Bearer Token
* **Respuesta (200 OK):** Lista de todos los análisis registrados.

#### 9. Detalle de Análisis Específico con Análisis Cromático (RF-22 / RN-47, RN-48)
* **`GET /api/v1/analisis/{id}`**
* **Seguridad:** Requiere Bearer Token
* **Respuesta (200 OK):** Objeto `AnalisisDetalleDTO` con porcentajes de píxeles (`pctDanado`, `pctSano`, `pctRegular`, `pctIncierto`).

#### 10. Resumen de Capturas de la Jornada Actual (RF-12 / RN-30, RN-31 / CU-M05)
* **`GET /api/v1/analisis/jornada`**
* **Seguridad:** Requiere Bearer Token
* **Respuesta (200 OK):** Capturas y análisis del día en curso (00:00 - 23:59).

---

### E. Zonas de Follaje (Capturas Aéreas con Dron)

#### 11. Listar Zonas de Follaje
* **`GET /api/v1/zonas-follaje`**
* **Seguridad:** Requiere Bearer Token

#### 12. Consultar Análisis de una Zona de Follaje
* **`GET /api/v1/zonas-follaje/{id}/analisis`**
* **Seguridad:** Requiere Bearer Token

---

### F. Analítica y Focos de Infestación (NOM-019-SEMARNAT-2017)

#### 13. Estadísticas Consolidadas para Dashboard Web (RF-21 / CU-W08 / CU-W19-CU-W23)
* **`GET /api/v1/estadisticas/dashboard`**
* **Seguridad:** Requiere Bearer Token
* **Respuesta (200 OK):**
  ```json
  {
    "totalArboles": 248,
    "totalSanos": 142,
    "totalInfestados": 61,
    "totalNoDeterminado": 45,
    "totalAnalisis": 412,
    "totalZonasFollaje": 18,
    "totalFocosActivos": 4,
    "distribucionPorEspecie": {
      "Fresno (Fraxinus uhdei)": 150,
      "Cedro blanco (Cupressus lusitanica)": 98
    },
    "distribucionPorEstado": {
      "sano": 142,
      "infestado": 61,
      "no_determinado": 45
    }
  }
  ```

#### 14. Detección de Focos Activos de Infestación (RN-25 / RN-27 / CU-W18 / RF-14)
* **`GET /api/v1/estadisticas/focos`**
* **Seguridad:** Requiere Bearer Token
* **Regla:** Detecta automáticamente clusters de $\ge 3$ árboles infestados en superficie de $1,000\text{ m}^2$.

---

### G. Servicio de Evidencias Fotográficas

#### 15. Descargar / Visualizar Imagen
* **`GET /api/v1/evidencias/{filename}`**
* **Seguridad:** Público (permite ser referenciado en etiquetas HTML `<img>`).
* **Respuesta (200 OK):** Binario de la imagen con encabezado `Content-Type: image/jpeg` o `image/png`.

#### 16. Subida Directa de Imagen (Multipart)
* **`POST /api/v1/evidencias/upload`**
* **Seguridad:** Requiere Bearer Token.
* **Form-Data:** `file: <archivo_imagen>`.
* **Respuesta (200 OK):** `{"filename": "...", "url": "/api/v1/evidencias/...", "sizeBytes": "..."}`.

---

### I. Servicio de Distribución de Modelos TFLite (PT-15, PI-07, PI-08)

#### 17. Consultar Última Versión del Modelo TFLite
* **`GET /api/v1/modelos/latest`**
* **Seguridad:** Público (permite a la app móvil validar su modelo local offline antes de iniciar jornada).
* **Respuesta (200 OK):**
  ```json
  {
    "version": "1.0.0",
    "nombreArchivo": "light_classifier.tflite",
    "tamanoBytes": 22557432,
    "sha256": "0a861b6c224729c50eae1fb1cf6c3b985146002a33b9eb419217ecf78bf9bb48",
    "urlDescarga": "/api/v1/modelos/descargar/light_classifier.tflite",
    "fechaPublicacion": "2026-05-01T00:00:00Z"
  }
  ```

#### 18. Descargar Binario del Modelo (.tflite)
* **`GET /api/v1/modelos/descargar/{nombreArchivo}`**
* **Seguridad:** Público.
* **Comportamiento Híbrido:**
  * En entorno **Local**: Transmite el flujo binario con `Content-Type: application/octet-stream`.
  * En entorno **AWS (S3)**: Redirige (`302 Found` / `303 See Other`) a la URL pública/firmada de Amazon S3 o CloudFront CDN.

---

### J. Servicio de Generación y Descarga de Reportes (RF-16, RF-17, RN-33, RN-34, PT-12, CU-W23)

#### 19. Descargar Reporte en Formato PDF
* **`GET /api/v1/reportes/pdf`**
* **Seguridad:** Requiere Bearer Token.
* **Parámetros Opcionales:** `fechaInicio`, `fechaFin`, `idZona`, `estadoSanitario`.
* **Regla RN-34:** Incluye obligatoriamente: ID árbol, nombre del árbol, fecha y hora de captura, resultado de clasificación, coordenadas GPS y nombre del experto.
* **Respuesta (200 OK):** Documento binario estándar `application/pdf` listo para descarga y visualización en navegadores y visores PDF.

#### 20. Consultar Datos Estructurados de Reportes
* **`GET /api/v1/reportes/datos`**
* **Seguridad:** Requiere Bearer Token.
* **Respuesta (200 OK):** JSON estructurado con la información consolidada y estadísticas para la vista previa en el dashboard web de Angular.

---

### K. Módulo de Administración (CU-A00) — Exclusivo Rol "Administrador"

| Método | Endpoint | Caso de Uso | Descripción |
|---|---|---|---|
| `GET` | `/api/v1/admin/cuentas` | CU-A01 | Lista todas las cuentas registradas. |
| `POST` | `/api/v1/admin/cuentas` | CU-A02 | Crea cuenta de usuario (valida correo único `MSG31`). |
| `PUT` | `/api/v1/admin/cuentas/{id}` | CU-A03 / CU-A05 | Actualiza datos y/o rol de una cuenta. |
| `PATCH`| `/api/v1/admin/cuentas/{id}/desactivar`| CU-A04 | Cambia estado de cuenta a `desactivada` (`MSG4`). |
| `GET` | `/api/v1/admin/dispositivos` | CU-A06 | Lista dispositivos móviles registrados. |
| `POST`| `/api/v1/admin/dispositivos` | CU-A06 | Registra un nuevo dispositivo móvil autorizado. |
| `POST`| `/api/v1/admin/cuentas/{idCuenta}/dispositivos/{idDisp}` | CU-A07 | Asocia dispositivo a cuenta (valida `MSG33`). |
| `DELETE`| `/api/v1/admin/cuentas/{idCuenta}/dispositivos/{idDisp}`| CU-A08 | Desasocia dispositivo (`MSG35`). |
| `GET` | `/api/v1/admin/cuentas/{idCuenta}/dispositivos` | CU-A09 | Lista dispositivos asociados a una cuenta específica. |

---

## 5. Modelos de Machine Learning Disponibles en el Repo

Ubicados en la carpeta [`models/`](file:///Users/dam/Documents/TTMobile/models):
1. **`light_classifier.tflite`** (21.5 MB): Modelo optimizado para dispositivos móviles Android (con cuantización float16), listo para integrarse en la app móvil.
2. **`classifier.keras`** (44.3 MB): Modelo de referencia entrenado en Keras.
3. **`entrenamiento_efficientnetb3.py`**: Pipeline de entrenamiento con EfficientNet-B3.
4. **`test.py`**: Script de prueba e inferencia.

---

## 6. Integración con AWS (Amazon S3 y AWS Lambda Serverless) — Preparación Completa

El backend está diseñado y preparado conforme a los lineamientos del **Trabajo Terminal (§4.4.2 y §8.2)** para operar en dos modalidades sin modificar una sola línea de código de lógica de negocio:

### 1. Abstracción de Almacenamiento (`AlmacenamientoService`)
* **`LocalAlmacenamientoService` (Por defecto):** Guarda y sirve imágenes en `uploads/evidencias/` y modelos en `models/`. No requiere cuenta ni credenciales de AWS.
* **`S3AlmacenamientoService` (Nube):** Se activa automáticamente estableciendo `aws.s3.enabled: true` (o variable de entorno `AWS_S3_ENABLED=true`). Sube evidencias al bucket de S3 (`descortezadores-evidencias-2026/evidencias/...`) y retorna URLs de S3 o CloudFront CDN.

### 2. Soporte Serverless para AWS Lambda (`LambdaHandler`)
* Implementado en [`LambdaHandler.java`](file:///Users/dam/Documents/TT/TT_BKN/backend/src/main/java/com/descortezadores/infrastructure/aws/LambdaHandler.java) heredando de `ApiGatewayProxyRequestEventFunction`.
* **Configuración en AWS Lambda:**
  * **Handler:** `com.descortezadores.infrastructure.aws.LambdaHandler::handleRequest`
  * **Runtime:** Java 21 (Corretto 21)
  * **Arquitectura:** `arm64` (AWS Graviton2)
  * **Memoria:** 512 MB
* **Empaquetado listo:** `./gradlew shadowJar` genera `build/libs/backend-0.1-all.jar` listo para subir a la consola de AWS Lambda o desplegar vía AWS SAM / CDK / Terraform.

### 3. Variables de Entorno para AWS
| Variable | Descripción | Valor por Defecto |
|---|---|---|
| `AWS_REGION` | Región de AWS | `us-east-1` |
| `AWS_S3_ENABLED` | Habilitar Amazon S3 | `false` (usa local) |
| `AWS_S3_BUCKET` | Nombre del Bucket S3 | `descortezadores-evidencias-2026` |
| `AWS_S3_CDN_URL` | URL de CloudFront CDN | Vacío |
| `RDS_ENDPOINT` | Host de Amazon RDS PostgreSQL | `localhost` |
| `RDS_DB_NAME` | Nombre de base de datos RDS | `descortezadores_db` |
| `RDS_USERNAME` | Usuario RDS | `postgres` |
| `RDS_PASSWORD` | Contraseña RDS | `postgres` |

---

## 7. Guía Maestra y Especificación Técnica para la Aplicación Móvil Android ("EcoVerde")

> **Destinatario:** Esta sección contiene toda la información técnica, arquitectónica y de diseño para el desarrollador o agente de IA encargado de implementar la aplicación móvil Android en el siguiente chat.

### 7.1 Visión General y Alcance Operativo
* **Nombre de la Aplicación:** **EcoVerde** (mencionado en §8.9, pág. 122).
* **Propósito:** Herramienta de campo para el **Experto de campo / Guardabosques**, que permite capturar fotografías de corteza y follaje de árboles en el **Bosque de Chapultepec**, realizar inferencia inmediata y análisis de color sin conexión a internet (**Offline-First**), georreferenciar árboles y sincronizar los datos automáticamente con la nube al detectar conexión.
* **Ubicación en el repositorio:** Carpeta `mobile/` en la raíz del proyecto.
* **Modelo TFLite listo en el repo:** [`models/light_classifier.tflite`](file:///Users/dam/Documents/TT/TT_BKN/models/light_classifier.tflite) (21.5 MB, cuantización float16, entrada $1 \times 300 \times 300 \times 3$).

---

### 7.2 Stack Tecnológico Oficial (Capítulo 7 y 8)

| Componente | Tecnología | Justificación y Rol en el Sistema |
|---|---|---|
| **Lenguaje** | **Kotlin 1.9+ / 2.0+** | Estándar de Android, corrutinas y Flows para concurrencia asíncrona fluida. |
| **Interfaz (UI)** | **Jetpack Compose + Material 3** | UI declarativa moderna (sin XML); soporta tema claro y oscuro de forma nativa. |
| **Arquitectura** | **MVVM (Model-View-ViewModel)** | Separación estricta entre capa de presentación (Compose), lógica de estado (ViewModels) y persistencia/red (Repositories). |
| **Base de Datos Local** | **SQLite vía Room** | Almacenamiento local persistente para árboles, capturas, análisis y estados de sincronización. |
| **Inferencia On-Device** | **TensorFlow Lite (`org.tensorflow:tensorflow-lite`)** | Clasificación binaria local inmediata en <1.5 s sin depender de conectividad. |
| **Procesamiento de Imagen** | **OpenCV para Android** | Análisis cromático HSV local concurrente a TFLite, recorte y normalización. |
| **Mapas Offline** | **MapLibre GL Android + PMTiles** | Renderizado vectorial offline del Bosque de Chapultepec mediante archivo `.pmtiles` local sin consumir datos móviles. |
| **Networking & API** | **Retrofit 2 + OkHttp 4** | Cliente HTTP REST con `OkHttpClient` e inyección automática de cabecera `Authorization: Bearer <jwt>`. |
| **Sincronización en Background** | **Android WorkManager** | Tarea en segundo plano con restricción de red (`NetworkType.CONNECTED`) para sincronización ascendente automática. |

---

### 7.3 Base de Datos Local SQLite (Room) — Esquema Offline-First

Para cumplir con **RN-50** (*la base de datos local SQLite no debe almacenar imágenes como BLOB; solo metadatos y ruta en almacenamiento interno*), Room debe estructurarse con las siguientes entidades:

```
[ArbolEntity] 1 ──── N [AnalisisEntity] 1 ──── 1 [AnalisisCromaticoEntity]
                              │ N
                              │
                              └─── 1 [ZonaFollajeEntity] (opcional si es follaje)
```

#### 1. `ArbolEntity` (Tabla `arbol_local`)
* `idArbol`: `String` (UUID PK).
* `nombreEspecifico`: `String` (ej. "Fresno Seccion 2 - Arbol 14").
* `especie`: `String` (ej. "Fresno (Fraxinus uhdei)", "Cedro blanco (Cupressus lusitanica)").
* `latitud`: `Double?`, `longitud`: `Double?`.
* `fechaRegistro`: `Long` (Epoch millis).
* `descripcion`: `String?`.
* `idCuenta`: `String`.
* `idDispositivo`: `String`.

#### 2. `AnalisisEntity` (Tabla `analisis_local`)
* `idAnalisis`: `String` (UUID PK).
* `rutaImagenLocal`: `String` (Ruta absoluta en almacenamiento interno privado: `context.filesDir/evidencias/{uuid}.jpg`).
* `latitudCaptura`: `Double?`, `longitudCaptura`: `Double?`.
* `fechaCaptura`: `Long` (Epoch millis).
* `estadoSincronizacion`: `String` (`pendiente`, `sincronizado`, `error`).
* `tipoImagen`: `String` (`corteza` o `follaje`).
* `resultadoClasificacion`: `String` (`sano`, `infestado`, `no_determinado`).
* `confianza`: `Double`.
* `observaciones`: `String?`.
* `idArbol`: `String?` (FK hacia `arbol_local`, obligatorio para corteza RN-37).
* `idZona`: `String?` (FK hacia `zona_follaje_local`, para capturas macro de follaje).

#### 3. `AnalisisCromaticoEntity` (Tabla `analisis_cromatico_local`)
* `idAnalisisCromatico`: `String` (UUID PK).
* `idAnalisis`: `String` (FK hacia `analisis_local`).
* `pctIncierto`: `Double`, `pctDanado`: `Double`, `pctRegular`: `Double`, `pctSano`: `Double`.

#### 4. `ZonaFollajeEntity` (Tabla `zona_follaje_local`)
* `idZona`: `String` (UUID PK).
* `nombreZona`: `String`, `latitudCentro`: `Double`, `longitudCentro`: `Double`, `radioMetros`: `Double`.

#### 5. `DispositivoConfigEntity` (DataStore o Room)
* `idDispositivo`: UUID único asignado al teléfono móvil.
* `tokenAuth`: JWT Bearer obtenido en login.
* `idCuenta`: UUID del usuario técnico.
* `nombreExperto`: Nombre completo del técnico.

---

### 7.4 Pipeline de Inferencia On-Device y Análisis de Color (Paso a Paso)

Este proceso ocurre **100% de forma local y en menos de 2 segundos** tras capturar la foto (RN-22, RN-46):

```
Captura Cámara/Galería
         │
         ▼
[1. Validación de Calidad (RN-07 a RN-09)]
   - Formato soportado: JPG, PNG, WEBP.
   - Tamaño <= 10 MB.
   - Resolución >= 300x300 px.
   - Herramienta de recorte opcional (RN-10 a RN-12).
         │
         ├─────────────────────────────────────────┐
         ▼                                         ▼
[2. Inferencia TensorFlow Lite]            [3. Análisis Cromático con OpenCV (RN-46)]
   - Modelo: light_classifier.tflite          - Conversión RGB -> HSV.
   - Entrada: 300x300x3 float32               - Si es Corteza (RN-48):
   - Valores: [0.0, 255.0]                      * Blanco/crema/rojizo (grumos resina).
   - Invocación: interpreter.run()              * Café-anaranjado (exudados).
   - Probabilidad P in [0.0, 1.0]:              * Grisáceo/sano (corteza natural).
       * Si P >= 0.5 -> "infestado"           - Si es Follaje (RN-47):
       * Si P < 0.5  -> "sano"                  * Verde (sano: Hue 70-160).
   - Regla RN-24:                               * Verde-amarillento (deterioro: Hue 40-70).
       * Si confianza < 0.75 (75%) ->           * Café-rojizo (muerte: Hue < 40 o > 330).
         Resultado forzoso = "no_determinado" - Salida: % sano, regular, dañado, incierto.
         Recomendar repetir captura.
         │                                         │
         └────────────────────┬────────────────────┘
                              ▼
[4. Consolidación de Resultados en Pantalla (RN-22, RN-26)]
   - Etiqueta visible obligatoria: "Offline" (RN-22).
   - Código de colores: Verde = Sano, Rojo = Infestado, Amarillo/Naranja = No determinado.
   - Pestaña 1: Clasificación e índice de confianza + Señales detectadas.
   - Pestaña 2: Análisis de color (Gráfica de barras de porcentajes HSV).
   - Guardado en Room con estado "pendiente" y foto en context.filesDir.
```

---

### 7.5 Contrato de Endpoints del Backend para el Móvil

El cliente móvil interactúa con los siguientes endpoints expuestos en el backend (probados y activos):

#### 1. Iniciar Sesión (`POST /login`)
* **Request:** `{"username": "experto@chapultepec.gob.mx", "password": "Password123!"}`
* **Response (200 OK):** Devuelve `access_token` (JWT propio), `roles`, `id_cuenta`, `nombre_completo`.
* **Regla RN-03:** La sesión en el móvil **no expira por inactividad**; persiste en DataStore hasta cierre manual.

#### 2. Sincronización de Lotes (`POST /api/v1/sync/detecciones`)
* **Headers:** `Authorization: Bearer <jwt>`, `Content-Type: application/json`
* **Trigger:** Disparado por `WorkManager` al detectar conectividad a internet (Wi-Fi o datos).
* **Payload:** Lista de `SyncItemDTO`:
  ```json
  [
    {
      "idAnalisisLocal": "c0000000-0000-0000-0000-000000000001",
      "idArbol": "b0000000-0000-0000-0000-000000000001",
      "nombreArbolNuevo": null,
      "latitud": 19.4215,
      "longitud": -99.1824,
      "fechaCaptura": "2026-10-16T10:30:00Z",
      "tipoImagen": "corteza",
      "resultadoClasificacion": "infestado",
      "confianza": 0.94,
      "observaciones": "Presencia de grumos de resina fresca",
      "pctIncierto": 0.05,
      "pctDanado": 0.70,
      "pctRegular": 0.15,
      "pctSano": 0.10,
      "imagenBase64": "/9j/4AAQSkZJRgABAQ...",
      "idDispositivo": "2b799a40-0dcb-4f9a-916f-260252b72f61"
    }
  ]
  ```
* **Respuesta (200 OK):**
  ```json
  {
    "totalRecibidos": 1,
    "totalSincronizados": 1,
    "idsSincronizados": ["c0000000-0000-0000-0000-000000000001"],
    "errores": []
  }
  ```
* **Acción en el móvil al recibir 200 OK:** Actualizar en Room `estadoSincronizacion = "sincronizado"`.
* **Regla RN-20:** En caso de fallo de red, reintentar automáticamente hasta 3 veces antes de marcar como `error`.
* **Regla RN-35:** Los registros locales pueden depurarse automáticamente de la memoria del teléfono **únicamente** si su estado es `sincronizado`.

#### 3. Resumen de Jornada (`GET /api/v1/analisis/jornada`)
* **Uso:** Alimenta la tarjeta superior del Dashboard móvil con el recuento de capturas del día (00:00 a 23:59) del técnico en turno (RN-30, RN-31).

#### 4. Descarga y Actualización del Modelo TFLite (PT-15, PI-07, PI-08)
* **Paso A: Comprobar versión (`GET /api/v1/modelos/latest`):**
  Devuelve la versión actual (`"1.0.0"`), tamaño y hash SHA-256. Si el hash local difiere, descargar la nueva versión.
* **Paso B: Descargar binario (`GET /api/v1/modelos/descargar/light_classifier.tflite`):**
  Descarga el archivo `.tflite` actualizado y lo almacena en `context.filesDir/models/` para reemplazar el modelo embebido.

#### 5. Alertas de Proximidad a Focos NOM-019 (`GET /api/v1/estadisticas/focos`)
* **Uso:** El móvil descarga la lista de focos activos al iniciar jornada.
* **Regla RN-27 y RN-28:** Cuando el GPS del teléfono detecta que el técnico está a $\le 100\text{ metros}$ de un foco activo, emite una notificación sonora/visual localmente sin requerir internet.
* **Regla RN-29:** Máximo 1 alerta por foco en un lapso de 60 minutos para evitar notificaciones repetitivas.

---

### 7.6 Diseño Visual, Paleta de Colores y Tipografía (Capítulo 8, págs. 122–126)

#### Paleta de Colores Oficial (Material Design 3):
```kotlin
val GreenPrimary          = Color(0xFF2D6A3E) // Verde bosque principal
val GreenPrimaryContainer = Color(0xFFB0F2BE) // Fondo verde contenedor
val RedInfestado          = Color(0xFFB3261E) // Rojo alerta (árbol infestado)
val GreenSano             = Color(0xFF2D6A3E) // Verde sano
val OrangeNoDeterminado   = Color(0xFF7A5232) // Naranja tierra / Sin revisar
val OrangeContainer       = Color(0xFFFFDCC1) // Fondo naranja claro
val SurfaceLight          = Color(0xFFF1F5ED) // Fondo de pantalla claro
val SurfaceVariant        = Color(0xFFE0E4DC) // Tarjetas y divisiones
```

#### Tipografía:
* **Roboto Flex** (tipografía estándar en Material 3).
* Soporte nativo para Tema Claro (Light Theme) y Tema Oscuro (Dark Theme) para adaptarse a la radiación solar directa o sombra en el bosque.

#### Navegación Principal (Scaffold con BottomBar):
1. **Pestaña 1: Inicio (Dashboard):** Tarjeta con resumen de jornada actual (total capturas, sanos, infestados) y lista de árboles registrados recientemente (Figura 42).
2. **Pestaña 2: Mapa:** Mapa de MapLibre GL con pines de colores por estado sanitario y botón de centrado en GPS actual (Figura 44).
3. **Botón Central Flotante (FAB):** Botón grande destacado **"Nueva Captura"** (Figura 45), que abre el flujo de 4 pasos:
   * *Paso A:* Selección de tipo de muestra: `Corteza` o `Follaje`.
   * *Paso B:* Selección de árbol: asociar a uno existente o presionar `+ Registrar árbol nuevo`.
   * *Paso C:* Captura con cámara (con recuadro guía para encuadre a 50-80 cm) o galería.
   * *Paso D:* Pantalla de resultados (clasificación, confianza, etiqueta "Offline", análisis de color).
4. **Pestaña 3: Alertas:** Lista de focos cercanos a < 100 m y registros con error de sincronización con botón "Reintentar ahora" (Figura 50).
5. **Pestaña 4: Perfil:** Datos del técnico, estadísticas de árboles monitoreados y botón "Cerrar sesión" (Figura 51).

---

### 7.7 Checklist de Implementación para el Agente Móvil

* [ ] Crear proyecto Android con Kotlin DSL (`build.gradle.kts`), Min SDK 26, Target SDK 34/35.
* [ ] Incluir dependencias: `androidx.compose.material3`, `androidx.room:room-ktx`, `org.tensorflow:tensorflow-lite`, `opencv-android`, `maplibre-android`, `com.squareup.retrofit2:retrofit`, `androidx.work:work-runtime-ktx`.
* [ ] Copiar [`models/light_classifier.tflite`](file:///Users/dam/Documents/TT/TT_BKN/models/light_classifier.tflite) a `app/src/main/assets/`.
* [ ] Crear las entidades Room (`ArbolEntity`, `AnalisisEntity`, `AnalisisCromaticoEntity`, `ZonaFollajeEntity`).
* [ ] Implementar `TFLiteClassifier` con entrada $300 \times 300 \times 3$ y salida binaria con umbral 75% (`RN-24`).
* [ ] Implementar `ColorAnalysisHelper` con OpenCV en espacio HSV para corteza y follaje (`RN-46` a `RN-49`).
* [ ] Configurar `SyncWorker` con `WorkManager` para consumir `POST /api/v1/sync/detecciones`.
* [ ] Implementar pantallas Compose con la paleta de colores Material 3 oficial.
* [ ] Probar flujo completo: Captura offline $\to$ Inferencia local $\to$ Conexión $\to$ Sincronización exitosa con backend.
