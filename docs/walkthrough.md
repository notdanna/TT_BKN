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
│   │   └── SyncDeteccionUseCase.java      # Flujo de sincronización móvil -> nube (§8.2.5)
│   ├── detection/
│   │   └── RegistrarDeteccionUseCase.java # Creación y registro de árboles
│   ├── history/
│   │   └── ConsultarHistorialUseCase.java # Consultas de árboles y detecciones
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
│   │   └── ErrorResponseDTO.java
│   └── controllers/               # Controladores REST HTTP
│       ├── HealthController.java     # GET /api/v1/health
│       ├── SyncController.java       # POST /sync y POST /api/v1/sync/detecciones
│       ├── ArbolController.java      # GET y POST /api/v1/arboles
│       ├── AdminController.java      # Endpoints administrativos CU-A00
│       └── EvidenciaController.java  # GET y POST /api/v1/evidencias
└── infrastructure/
    ├── persistence/               # Interfaces Micronaut Data JDBC
    │   ├── CuentaRepository.java
    │   ├── DispositivoRepository.java
    │   ├── CuentaDispositivoRepository.java
    │   ├── ArbolRepository.java
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

---

### D. Servicio de Evidencias Fotográficas

#### 6. Descargar / Visualizar Imagen
* **`GET /api/v1/evidencias/{filename}`**
* **Seguridad:** Público (permite ser referenciado en etiquetas HTML `<img>`).
* **Respuesta (200 OK):** Binario de la imagen con encabezado `Content-Type: image/jpeg` o `image/png`.

#### 7. Subida Directa de Imagen (Multipart)
* **`POST /api/v1/evidencias/upload`**
* **Seguridad:** Requiere Bearer Token.
* **Form-Data:** `file: <archivo_imagen>`.
* **Respuesta (200 OK):** `{"filename": "...", "url": "/api/v1/evidencias/...", "sizeBytes": "..."}`.

---

### E. Módulo de Administración (CU-A00) — Exclusivo Rol "Administrador"

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

## 6. Siguiente Fase de Desarrollo (Roadmap para el siguiente Agente / Chat)

El siguiente paso es iniciar con la **Aplicación Móvil Android**:
1. Crear el proyecto Android en `mobile/` con **Kotlin** y **Jetpack Compose** (Material 3).
2. Colocar `light_classifier.tflite` en `app/src/main/assets/`.
3. Configurar **Room** (SQLite local) con las entidades locales equivalentes a `arbol`, `analisis`, `analisis_cromatico` (Offline-First).
4. Implementar cliente **Retrofit + OkHttp** apuntando a `POST /sync` para la sincronización con este backend.
