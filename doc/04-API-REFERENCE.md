# 04 · Referencia Completa de la API REST

> Todos los endpoints externos se consumen a través del **API Gateway** en `http://localhost:8080`.
> Salvo los endpoints públicos indicados, todas las peticiones requieren el header `Authorization: Bearer <access_token>`.

---

## 1. Resumen de Rutas

| Prefijo en el Gateway | Destino (Eureka `lb://`) | Puerto directo | Descripción |
|---|---|---|---|
| `/api/gimnasio` | API Gateway (local) | `8080` | Health check público del Gateway |
| `/api/gimnasio/agregacion/**` | API Gateway (WebClient) | `8080` | Agregación reactiva (`miembro` + `clases` + `pagos`) |
| `/api/gimnasio/miembros/**` | `lb://miembro-service` | `8083` | Socios e historial de entrenamientos |
| `/api/gimnasio/entrenadores/**` | `lb://entrenador-service` | `8081` | Instructores del gimnasio |
| `/api/gimnasio/clases/**` | `lb://clase-service` | `8084` / `8088` | Programación de clases, horarios y aforo |
| `/api/gimnasio/equipos/**` | `lb://equipo-service` | `8082` | Inventario de máquinas y reporte de averías |
| `/api/gimnasio/pagos/**` | `lb://pago-service` | `8086` | Procesamiento asíncrono de pagos y DLQ |
| `/api/gimnasio/monitoreo/**` | `lb://monitoreo-service` | `8087` | Ocupación en vivo (SSE), Kafka Streams y checkpoints |

---

## 2. API Gateway y Agregación (`:8080`)

### 2.1 `GET /api/gimnasio`
Verifica que el API Gateway está activo.
- **Autenticación:** Pública (sin token)
- **Respuesta `200 OK`:**
  ```text
  API is working
  ```

### 2.2 `GET /api/gimnasio/agregacion/miembro/{id}`
Consolida de forma concurrente y no bloqueante (`Mono.zip`) la información de un miembro (`miembro-service`), las clases disponibles (`clase-service`) y los pagos registrados de ese miembro (`pago-service`).
- **Roles permitidos:** `ADMIN`, `TRAINER`, `MEMBER` *(nota: usar `ADMIN` para incluir pagos, ya que `pago-service` restringe el listado de pagos a `ADMIN`)*.
- **Respuesta `200 OK`:**
  ```json
  {
    "miembro": {
      "id": 1,
      "nombre": "Juan Perez",
      "email": "juan@email.com",
      "fechaInscripcion": "2026-10-12"
    },
    "clasesDisponibles": [
      {
        "id": 1,
        "nombre": "Spinning",
        "horario": "2026-10-13T08:00:00",
        "capacidadMaxima": 20,
        "ocupacionActual": 0,
        "entrenador": {
          "id": 1,
          "nombre": "Carlos Ruiz",
          "especialidad": "Spinning"
        }
      }
    ],
    "pagosRegistrados": [
      {
        "id": 1,
        "miembroId": 1,
        "monto": 120000.00,
        "metodoPago": "TARJETA",
        "estado": "APROBADO",
        "intentos": 1,
        "motivoFallo": null,
        "fechaSolicitud": "2026-10-12T15:00:00",
        "fechaProcesamiento": "2026-10-12T15:00:01"
      }
    ],
    "mensaje": "Resumen consolidado generado por Spring Cloud Gateway"
  }
  ```

### 2.3 `GET /actuator/gateway/routes`
Lista todas las rutas activas en Spring Cloud Gateway (declarativas en YAML y programáticas en Java).
- **Autenticación:** Pública

---

## 3. Microservicio de Miembros (`miembro-service` · `:8083`)

### 3.1 `POST /api/gimnasio/miembros`
Registra un nuevo miembro en el gimnasio y publica el evento `MiembroInscritoEvent` en RabbitMQ (`gimnasio.miembro.exchange`).
- **Roles permitidos:** `ADMIN`, `MEMBER`
- **Body (`MiembroRequest`):**
  ```json
  {
    "nombre": "Carlos Mendoza",
    "email": "carlos@gmail.com",
    "fechaInscripcion": "2026-10-12"
  }
  ```
- **Validaciones:**
  - `nombre`: obligatorio (`@NotBlank`).
  - `email`: Value Object `Email` con validación de formato `^[^@\s]+@[^@\s]+\.[^@\s]+$`.
  - `fechaInscripcion`: Value Object `FechaInscripcion` en formato `YYYY-MM-DD` (si se omite, toma la fecha actual).
- **Respuestas:** `200 OK` (entidad `Miembro` creada) · `400 Bad Request` (datos o Value Object inválidos) · `403 Forbidden`.

### 3.2 `GET /api/gimnasio/miembros`
Obtiene la lista de todos los miembros inscritos.
- **Roles permitidos:** `ADMIN`, `TRAINER`
- **Respuestas:** `200 OK` (`List<Miembro>`) · `403 Forbidden`.

### 3.3 `GET /api/gimnasio/miembros/{id}`
Obtiene un miembro específico por su ID.
- **Roles permitidos:** `ADMIN`, `TRAINER`, `MEMBER`
- **Respuestas:** `200 OK` (`Miembro`) · `404 Not Found` (si el ID no existe).

### 3.4 `POST /api/gimnasio/miembros/{id}/entrenamientos`
Registra una sesión de entrenamiento del miembro y publica `DatosEntrenamiento` en el topic de Kafka `datos-entrenamiento` (key = `miembroId`).
- **Roles permitidos:** `ADMIN`, `TRAINER`, `MEMBER`
- **Body (`RegistroEntrenamientoRequest`):**
  ```json
  {
    "tipo": "CARDIO",
    "duracionMinutos": 45,
    "calorias": 400
  }
  ```
- **Validaciones:** `tipo` obligatorio, `duracionMinutos > 0`, `calorias >= 0`, y el miembro `{id}` debe existir.
- **Respuestas:** `200 OK` (`DatosEntrenamiento`) · `400 Bad Request` · `404 Not Found` · `503 Service Unavailable` (si Kafka no responde en 5 s).

---

## 4. Microservicio de Entrenadores (`entrenador-service` · `:8081`)

### 4.1 `POST /api/gimnasio/entrenadores`
Registra un nuevo entrenador.
- **Roles permitidos:** `ADMIN`
- **Body (`EntrenadorRequest`):**
  ```json
  {
    "nombre": "Elena Torres",
    "especialidad": "Pilates"
  }
  ```
- **Respuestas:** `200 OK` (`Entrenador`) · `400 Bad Request` · `403 Forbidden`.

### 4.2 `GET /api/gimnasio/entrenadores`
Lista todos los entrenadores registrados.
- **Roles permitidos:** `ADMIN`, `TRAINER`, `MEMBER`
- **Respuestas:** `200 OK` (`List<Entrenador>`).

### 4.3 `GET /api/gimnasio/entrenadores/{id}`
Consulta un entrenador por su ID (usado también internamente por `clase-service` para validar y enriquecer clases).
- **Roles permitidos:** `ADMIN`, `TRAINER`, `MEMBER`
- **Respuestas:** `200 OK` (`Entrenador`) · `404 Not Found`.

---

## 5. Microservicio de Clases (`clase-service` · `:8084` y `:8088`)

### 5.1 `POST /api/gimnasio/clases`
Programa una nueva clase validando previamente por REST que el `entrenadorId` exista en `entrenador-service`, y publica la ocupación inicial (`0`) en el topic de Kafka `ocupacion-clases`.
- **Roles permitidos:** `ADMIN`, `TRAINER`
- **Body (`ClaseRequest`):**
  ```json
  {
    "nombre": "Crossfit Avanzado",
    "horario": "2026-10-15T18:00:00",
    "capacidad": 20,
    "entrenadorId": 1
  }
  ```
- **Validaciones:**
  - `nombre`: obligatorio.
  - `horario`: Value Object `Horario` (ISO-8601).
  - `capacidad`: Value Object `Capacidad` (`> 0`).
  - `entrenadorId`: entero positivo que debe existir en `entrenador-service`.
- **Respuestas:** `200 OK` (`Clase`) · `400 Bad Request` · `404 Not Found` (entrenador inexistente) · `503 Service Unavailable` (`entrenador-service` caído).

### 5.2 `GET /api/gimnasio/clases`
Lista todas las clases programadas enriqueciendo cada una con los datos completos del entrenador (`EntrenadorDTO`).
- **Roles permitidos:** `ADMIN`, `TRAINER`, `MEMBER`
- **Respuesta `200 OK` (`List<ClaseResponse>`):**
  ```json
  [
    {
      "id": 1,
      "nombre": "Spinning",
      "horario": "2026-10-13T08:00:00",
      "capacidadMaxima": 20,
      "ocupacionActual": 3,
      "entrenador": {
        "id": 1,
        "nombre": "Carlos Ruiz",
        "especialidad": "Spinning"
      }
    }
  ]
  ```

### 5.3 `PUT /api/gimnasio/clases/{id}/horario`
Cambia el horario de una clase y difunde el evento `HorarioClaseCambiadoEvent` por RabbitMQ Fanout Exchange (`gimnasio.clase.horario.events`) hacia 3 colas simultáneas (App Móvil, Email y Auditoría).
- **Roles permitidos:** `ADMIN`, `TRAINER`
- **Body (`CambioHorarioClaseRequest`):**
  ```json
  {
    "nuevoHorario": "2026-10-20T19:30:00",
    "motivo": "Mantenimiento de salón principal"
  }
  ```
- **Respuestas:** `200 OK` (`Clase`) · `400 Bad Request` · `404 Not Found`.

### 5.4 `POST /api/gimnasio/clases/{id}/ingreso`
Incrementa en 1 la ocupación actual de la clase (con bloqueo pesimista JPA `findByIdForUpdate`) y, tras confirmar la transacción (`afterCommit`), publica el evento `OcupacionClase` en Kafka (`ocupacion-clases`).
- **Roles permitidos:** `ADMIN`, `TRAINER`, `MEMBER`
- **Respuestas:** `200 OK` (`Clase`) · `404 Not Found` · `409 Conflict` (cuando la clase ya alcanzó su capacidad máxima).

### 5.5 `POST /api/gimnasio/clases/{id}/salida`
Decrementa en 1 la ocupación actual de la clase y publica la actualización en Kafka (`ocupacion-clases`).
- **Roles permitidos:** `ADMIN`, `TRAINER`, `MEMBER`
- **Respuestas:** `200 OK` (`Clase`) · `404 Not Found` · `409 Conflict` (cuando la clase tiene `0` asistentes).

---

## 6. Microservicio de Equipos (`equipo-service` · `:8082`)

### 6.1 `POST /api/gimnasio/equipos`
Agrega un nuevo equipo al inventario.
- **Roles permitidos:** `ADMIN`
- **Body (`EquipoRequest`):**
  ```json
  {
    "nombre": "Bicicleta Estática Pro",
    "descripcion": "Bicicleta de spinning con monitor cardíaco",
    "cantidad": 10
  }
  ```
- **Validaciones:** `nombre` y `descripcion` obligatorios, `cantidad > 0`.
- **Respuestas:** `200 OK` (`Equipo`) · `400 Bad Request` · `403 Forbidden`.

### 6.2 `GET /api/gimnasio/equipos`
Lista todos los equipos del inventario (enrutado por la ruta programática `GatewayRoutesConfig`).
- **Roles permitidos:** `ADMIN`, `TRAINER`, `MEMBER`
- **Respuestas:** `200 OK` (`List<Equipo>`).

### 6.3 `POST /api/gimnasio/equipos/{id}/reportar-averia`
Registra una avería en el equipo y publica `EquipoAveriadoEvent` en el Fanout Exchange `gimnasio.equipo.events` hacia 3 colas simultáneas (Mantenimiento, Entrenadores y App Socios).
- **Roles permitidos:** `ADMIN`, `TRAINER`
- **Body (`ReporteAveriaRequest`):**
  ```json
  {
    "motivo": "Fallo en motor de tracción y banda rota",
    "gravedad": "ALTA"
  }
  ```
- **Respuestas:** `200 OK` (`Equipo` con descripción actualizada) · `400 Bad Request` · `404 Not Found`.

---

## 7. Microservicio de Pagos (`pago-service` · `:8086`)

### 7.1 `POST /api/gimnasio/pagos`
Registra una solicitud de pago en estado `PENDIENTE` y la encola en RabbitMQ (`gimnasio.pagos.exchange` → `pagos.procesamiento`) para procesamiento asíncrono con reintentos y Dead Letter Queue.
- **Roles permitidos:** `ADMIN`, `MEMBER`
- **Body (`PagoRequest`):**
  ```json
  {
    "miembroId": 1,
    "monto": 120000,
    "metodoPago": "TARJETA"
  }
  ```
  - Valores especiales para demostración de fallos y DLQ:
    - `"metodoPago": "TARJETA_RECHAZADA"` → lanza `PagoRechazadoException`, reintenta **3 veces** con backoff exponencial (1 s, 2 s) y luego cae en la DLQ (`FALLIDO`).
    - `"monto": -50` → lanza `PagoInvalidoException`, **no reintenta** (1 intento) y va directo a la DLQ (`FALLIDO`).
- **Respuesta:** `202 Accepted` (`Pago` en estado `PENDIENTE`).

### 7.2 `GET /api/gimnasio/pagos`
Lista todos los pagos con su estado (`PENDIENTE`, `APROBADO`, `FALLIDO`), número de intentos y motivo de fallo.
- **Roles permitidos:** `ADMIN`
- **Respuestas:** `200 OK` (`List<Pago>`) · `403 Forbidden`.

### 7.3 `GET /api/gimnasio/pagos/fallidos`
Lista únicamente los pagos que terminaron en la Dead Letter Queue (`estado = FALLIDO`).
- **Roles permitidos:** `ADMIN`
- **Respuestas:** `200 OK` (`List<Pago>`) · `403 Forbidden`.

---

## 8. Microservicio de Monitoreo (`monitoreo-service` · `:8087`)

### 8.1 `GET /api/gimnasio/monitoreo/ocupacion`
Devuelve la foto actual del dashboard en memoria alimentado por el consumidor de Kafka del topic `ocupacion-clases`.
- **Roles permitidos (vía Gateway):** `ADMIN`, `TRAINER`
- **Respuesta `200 OK`:**
  ```json
  [
    {
      "claseId": "1",
      "nombreClase": "Spinning",
      "ocupacionActual": 5,
      "capacidadMaxima": 20,
      "porcentajeOcupacion": 25.0,
      "timestamp": "2026-10-12T20:15:00Z"
    }
  ]
  ```

### 8.2 `GET /api/gimnasio/monitoreo/ocupacion/stream`
Stream **Server-Sent Events (`text/event-stream`)** que empuja en tiempo real cada cambio de ocupación recibido desde Kafka.
- **Roles permitidos (vía Gateway):** `ADMIN`, `TRAINER`

### 8.3 `GET /api/gimnasio/monitoreo/entrenamiento/{miembroId}/resumen`
Consulta interactiva (ReadOnlyKeyValueStore / WindowStore) al estado materializado de **Kafka Streams** (`resumen-entrenamiento-store`), que agrega las sesiones de entrenamiento de cada miembro en ventanas de **7 días**.
- **Roles permitidos (vía Gateway):** `ADMIN`, `TRAINER`
- **Respuesta `200 OK`:**
  ```json
  [
    {
      "miembroId": "1",
      "totalSesiones": 2,
      "totalMinutos": 105,
      "totalCalorias": 750,
      "sesionesPorTipo": {
        "CARDIO": 1,
        "FUERZA": 1
      },
      "ultimaActualizacion": "2026-10-12T20:20:00Z",
      "inicioVentana": "2026-10-08T00:00:00Z",
      "finVentana": "2026-10-15T00:00:00Z"
    }
  ]
  ```

### 8.4 `GET /api/gimnasio/monitoreo/recuperacion/estado`
Muestra los checkpoints guardados por partición (`topic-particion` → `ultimoOffset`) y el conteo de eventos procesados en H2 en archivo.
- **Roles permitidos:** `ADMIN`
- **Respuesta `200 OK`:**
  ```json
  {
    "checkpoints": [
      {
        "id": "ocupacion-clases-0",
        "topic": "ocupacion-clases",
        "particion": 0,
        "ultimoOffset": 4,
        "actualizado": "2026-10-12T15:30:00"
      }
    ],
    "eventosProcesados": 12,
    "ultimosEventos": [ ... ]
  }
  ```
