# Arquitectura: Microservicios Gimnasio

## Resumen

Refactorización de un monolito Spring Boot ("Gimnasio") en microservicios independientes bajo principios de **Domain-Driven Design (DDD)**: 5 servicios de negocio (clase, entrenador, equipo, miembro, pago), un servicio consumidor de eventos (notificacion) y un API Gateway como punto de entrada único. La comunicación es síncrona vía REST (gateway → servicios, clase → entrenador) y asíncrona vía RabbitMQ (eventos de inscripción, averías y procesamiento de pagos con Dead Letter Queue). Persistencia mediante H2 en memoria, aislada por servicio.

**Stack tecnológico:**
- **Runtime**: Java 17 (Spring Boot 3.3.2)
- **Build**: Maven (mvnw)
- **Persistencia**: H2 (JDBC, JPA)
- **API**: REST (Spring Web)
- **Seguridad**: Spring Security + OAuth2 Resource Server (JWT) + Keycloak 24
- **Documentación API**: SpringDoc OpenAPI 2.6.0 (Swagger UI)
- **Mensajería**: RabbitMQ 3 (Spring AMQP, serialización JSON con Jackson)
- **Orquestación**: Docker Compose
- **Dependencias comunes**: Lombok, Spring Data JPA

---

## Servicios

| Servicio | Puerto | Dominio | Responsabilidad | Entidad / Evento principal |
|---|---|---|---|---|
| **api-gateway** | 8080 | Gateway | Enrutamiento perimetral unificado hacia los microservicios | - |
| **clase-microservice** | 8084 | Programación | Crear y listar clases/sesiones de entrenamiento | `Clase` |
| **entrenador-microservice** | 8081 | Recursos Humanos | Gestionar entrenadores/instructores | `Entrenador` |
| **equipo-microservice** | 8082 | Equipamiento | Inventario, control y reporte de averías | `Equipo`, `EquipoAveriadoEvent` |
| **miembro-microservice** | 8083 | Membresía | Registrar miembros y emitir eventos de inscripción | `Miembro`, `MiembroInscritoEvent` |
| **notificacion-microservice**| 8085 | Notificaciones | Consumir eventos asincrónicos (bienvenida, tickets, app push) | Listeners RabbitMQ |
| **pago-microservice** | 8086 | Facturación | Recibir pagos, procesarlos de forma asíncrona y gestionar los fallidos vía DLQ | `Pago`, `PagoSolicitadoEvent` |
| **RabbitMQ** | 5672 / 15672 | Broker | Enrutamiento de colas y exchanges AMQP | Direct & Fanout Exchanges, Dead Letter Exchange |
| **Keycloak** | 8180 | Identidad | Proveedor de identidad (IAM): autenticación, autorización y emisión de tokens JWT | Realm `gimnasio`, roles, clients |

---

## Diagrama de Componentes y Mensajería Asincrónica

```mermaid
graph TB
    subgraph "Clientes Externos"
        CLIENT["Cliente / Postman / Frontend"]
    end

    subgraph "Identidad"
        KEYCLOAK["Keycloak (8180)<br/>Realm: gimnasio<br/>Tokens JWT"]
    end

    subgraph "API Gateway"
        GATEWAY["API Gateway (8080)<br/>Spring Security + JWT"]
    end

    subgraph "Microservicios de Negocio"
        CLASE["Clase Service (8084)"]
        ENTRENADOR["Entrenador Service (8081)"]
        EQUIPO["Equipo Service (8082)"]
        MIEMBRO["Miembro Service (8083)"]
        PAGO["Pago Service (8086)"]
    end

    subgraph "RabbitMQ Broker (5672 / 15672)"
        EX_DIRECT["Direct Exchange:<br/>gimnasio.miembro.exchange"]
        Q_MIEMBRO["Cola: miembro.inscripcion.notificacion"]

        EX_FANOUT["Fanout Exchange:<br/>gimnasio.equipo.events"]
        Q_MANT["Cola: equipo.averia.mantenimiento"]
        Q_ENTR["Cola: equipo.averia.entrenadores"]
        Q_APP["Cola: equipo.averia.app-socios"]

        EX_PAGOS["Direct Exchange:<br/>gimnasio.pagos.exchange"]
        Q_PAGOS["Cola: pagos.procesamiento<br/>(x-dead-letter-exchange)"]
        EX_DLX["Dead Letter Exchange:<br/>gimnasio.pagos.dlx"]
        Q_DLQ["DLQ: pagos.procesamiento.dlq"]
    end

    subgraph "Microservicio Consumidor"
        NOTIF["Notificacion Service (8085)"]
    end

    CLIENT -->|Token JWT| KEYCLOAK
    CLIENT -->|Authorization: Bearer token| GATEWAY
    GATEWAY -->|Valida JWT| KEYCLOAK
    GATEWAY --> CLASE
    GATEWAY --> ENTRENADOR
    GATEWAY --> EQUIPO
    GATEWAY --> MIEMBRO
    GATEWAY --> PAGO

    CLASE -->|REST GET /entrenadores/{id}<br/>propaga Authorization| ENTRENADOR
    ENTRENADOR -->|Valida JWT| KEYCLOAK

    %% Flujos Asincrónicos
    MIEMBRO -->|Publica MiembroInscritoEvent| EX_DIRECT
    EX_DIRECT -->|routingKey: miembro.inscrito| Q_MIEMBRO
    Q_MIEMBRO --> NOTIF

    EQUIPO -->|Publica EquipoAveriadoEvent| EX_FANOUT
    EX_FANOUT --> Q_MANT
    EX_FANOUT --> Q_ENTR
    EX_FANOUT --> Q_APP
    Q_MANT --> NOTIF
    Q_ENTR --> NOTIF
    Q_APP --> NOTIF

    PAGO -->|Publica PagoSolicitadoEvent| EX_PAGOS
    EX_PAGOS -->|routingKey: pago.procesar| Q_PAGOS
    Q_PAGOS -->|Consume con reintentos| PAGO
    Q_PAGOS -.->|Rechazo sin reencolar| EX_DLX
    EX_DLX -->|routingKey: pago.fallido| Q_DLQ
    Q_DLQ -->|Marca FALLIDO| PAGO
```

---

## Comunicación entre Servicios

### 1. Comunicación Síncrona (REST)
- `clase-microservice` → `entrenador-microservice`: Enriquecimiento de clases consultando datos del instructor.
- `api-gateway` → Todos los microservicios: Enrutamiento perimetral con `RestClient`.

### 2. Comunicación Asíncrona con RabbitMQ (Parte 2)
- **Direct Exchange (Notificación de Inscripción)**:
  - **Exchange**: `gimnasio.miembro.exchange`
  - **Routing Key**: `miembro.inscrito`
  - **Cola**: `miembro.inscripcion.notificacion`
  - **Comportamiento**: Al registrar un miembro en `miembro-microservice`, se emite `MiembroInscritoEvent`. `notificacion-microservice` lo consume de forma desacoplada y simula el correo de bienvenida.
- **Fanout Exchange (Patrón Pub/Sub - Avería de Equipos)**:
  - **Exchange**: `gimnasio.equipo.events`
  - **Comportamiento**: Cuando un equipo sufre una falla o requiere mantenimiento urgente (`POST /api/gimnasio/equipos/{id}/reportar-averia`), `equipo-microservice` publica `EquipoAveriadoEvent`. RabbitMQ difunde el evento a 3 colas suscritas en simultáneo:
    1. `equipo.averia.mantenimiento`: Crea ticket urgente para la cuadrilla técnica y proveedor de repuestos.
    2. `equipo.averia.entrenadores`: Alerta a los instructores de sala para reprogramar rutinas.
    3. `equipo.averia.app-socios`: Dispara notificación a la app móvil de clientes sobre máquina fuera de servicio.

- **Dead Letter Queue (Procesamiento de Pagos)**:
  - **Exchange / Cola principal**: `gimnasio.pagos.exchange` → `pagos.procesamiento` (routing key `pago.procesar`), declarada con `x-dead-letter-exchange=gimnasio.pagos.dlx` y `x-dead-letter-routing-key=pago.fallido`.
  - **DLX / DLQ**: `gimnasio.pagos.dlx` → `pagos.procesamiento.dlq`.
  - **Comportamiento**: `POST /api/gimnasio/pagos` guarda el pago como `PENDIENTE`, publica `PagoSolicitadoEvent` y responde `202 Accepted`. El consumidor cobra contra una pasarela simulada:
    1. Éxito → `APROBADO`.
    2. `PagoRechazadoException` (pasarela rechaza, simulado con `metodoPago = TARJETA_RECHAZADA`) → reintento con backoff exponencial (1s, 2s) hasta 3 intentos.
    3. `PagoInvalidoException` (monto ≤ 0 o sin miembro) → error permanente, sin reintentos.
    4. Agotados los intentos, `RejectAndDontRequeueRecoverer` rechaza el mensaje sin reencolar y RabbitMQ lo desvía al DLX. El consumidor de la DLQ lee la cabecera `x-death` (cola de origen y razón) y marca el pago como `FALLIDO`, consultable en `GET /api/gimnasio/pagos/fallidos`.
  - **Configuración**: `pago.procesamiento.max-intentos`, `pago.procesamiento.intervalo-inicial-ms`, `pago.procesamiento.multiplicador` y `pago.dlq.consumidor-activo` (en `false` deja los mensajes en la DLQ para inspeccionarlos en el panel de RabbitMQ).
  - **Nota**: productor y consumidor viven en el mismo servicio porque el procesamiento asíncrono es parte del contexto de Facturación; la cola desacopla la recepción del pago de la llamada a la pasarela.

### 3. Garantías de entrega
- **Declaración en productor y consumidor**: exchanges, colas y bindings se declaran tanto en el servicio que publica (`miembro`, `equipo`) como en `notificacion-microservice` (`pago-microservice` es productor y consumidor, declara todo en un solo lugar). La declaración es idempotente, así que si el consumidor no ha arrancado los eventos quedan encolados en vez de descartarse por falta de binding.
- **Arranque ordenado**: en `docker-compose.yml` RabbitMQ tiene `healthcheck` (`rabbitmq-diagnostics ping`) y los servicios que lo usan esperan con `condition: service_healthy`.

---

## Seguridad (Keycloak + JWT)

### Visión General
Todos los microservicios (excepto `notificacion-microservice`) están protegidos con **Spring Security + OAuth2 Resource Server** validando tokens JWT emitidos por **Keycloak 24**. El API Gateway valida el token en la entrada y propaga el header `Authorization` a los servicios internos.

### Realm `gimnasio`
- **Nombre**: `gimnasio`
- **Configuración**: importado automáticamente al iniciar Keycloak via `--import-realm`
- **Archivo**: `keycloak/realm/gimnasio-realm.json`
- **Token lifespan**: 300 segundos (5 minutos)

### Roles

| Rol | Descripción |
|---|---|
| `ROLE_ADMIN` | Administrador del sistema con acceso total |
| `ROLE_TRAINER` | Entrenador del gimnasio |
| `ROLE_MEMBER` | Miembro/socio del gimnasio |

### Clients

| Client ID | Tipo | Secret | Propósito |
|---|---|---|---|
| `gimnasio-gateway` | public | — | Frontend/Postman → obtiene token con username+password (Resource Owner Password) |
| `gimnasio-entrenador` | confidential | `entrenador-secret-2026` | Microservicio de entrenadores |
| `gimnasio-equipo` | confidential | `equipo-secret-2026` | Microservicio de equipos |
| `gimnasio-miembro` | confidential | `miembro-secret-2026` | Microservicio de miembros |
| `gimnasio-clase` | confidential | `clase-secret-2026` | Microservicio de clases |
| `gimnasio-pago` | confidential | `pago-secret-2026` | Microservicio de pagos |
| `gimnasio-notificacion` | confidential | `notificacion-secret-2026` | Microservicio de notificaciones |

### Usuarios de Prueba

| Usuario | Contraseña | Roles |
|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` |
| `entrenador1` | `trainer123` | `ROLE_TRAINER` |
| `miembro1` | `member123` | `ROLE_MEMBER` |

### Obtener Token JWT
```bash
# Obtener token para admin (RESOURCE OWNER PASSWORD CREDENTIALS)
curl -X POST http://localhost:8180/realms/gimnasio/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gimnasio-gateway" \
  -d "username=admin" \
  -d "password=admin123"
# El access_token se usa como Authorization: Bearer <token>
```

### Integración por Microservicio
Cada microservicio (excepto `notificacion`) incluye:
- **`SecurityConfig.java`**: Configura el `SecurityFilterChain` con `oauth2ResourceServer().jwt()`. Extrae roles del claim `realm_access.roles` del JWT de Keycloak y los mapea a authorities Spring Security con prefijo `ROLE_`.
- **`application.properties`**: `spring.security.oauth2.resourceserver.jwt.issuer-uri` y `jwk-set-uri` apuntando a Keycloak.

### Propagación de Token
- **API Gateway → microservicios**: `RestClientConfig.java` agrega un request interceptor que lee el header `Authorization` del request entrante y lo reenvía al microservicio destino.
- **clase-microservice → entrenador-microservice**: `ClaseService.java` propaga el header `Authorization` al hacer `RestTemplate.exchange()`.

### Autorización por Endpoint

| Endpoint | Método | Roles Permitidos |
|---|---|---|
| `GET /api/gimnasio` | GET | Público (health check) |
| `GET /api/gimnasio/entrenadores` | GET | ADMIN, TRAINER, MEMBER |
| `POST /api/gimnasio/entrenadores` | POST | ADMIN |
| `GET /api/gimnasio/equipos` | GET | ADMIN, TRAINER, MEMBER |
| `POST /api/gimnasio/equipos` | POST | ADMIN |
| `POST /api/gimnasio/equipos/{id}/reportar-averia` | POST | ADMIN, TRAINER |
| `GET /api/gimnasio/miembros` | GET | ADMIN, TRAINER |
| `POST /api/gimnasio/miembros` | POST | ADMIN, MEMBER |
| `GET /api/gimnasio/clases` | GET | ADMIN, TRAINER, MEMBER |
| `POST /api/gimnasio/clases` | POST | ADMIN, TRAINER |
| `GET /api/gimnasio/pagos` | GET | ADMIN |
| `POST /api/gimnasio/pagos` | POST | ADMIN, MEMBER |
| `GET /api/gimnasio/pagos/fallidos` | GET | ADMIN |

---

## Documentación de APIs (Swagger/OpenAPI)

Cada microservicio expone Swagger UI y la definición OpenAPI 3.0:

| Servicio | Swagger UI | OpenAPI JSON |
|---|---|---|
| API Gateway | `http://localhost:8080/swagger-ui.html` | `http://localhost:8080/v3/api-docs` |
| Entrenador | `http://localhost:8081/swagger-ui.html` | `http://localhost:8081/v3/api-docs` |
| Equipo | `http://localhost:8082/swagger-ui.html` | `http://localhost:8082/v3/api-docs` |
| Miembro | `http://localhost:8083/swagger-ui.html` | `http://localhost:8083/v3/api-docs` |
| Clase | `http://localhost:8084/swagger-ui.html` | `http://localhost:8084/v3/api-docs` |
| Pago | `http://localhost:8086/swagger-ui.html` | `http://localhost:8086/v3/api-docs` |

Cada Swagger UI está configurado con el security scheme **Bearer JWT** → puede probar endpoints protegidos ingresando el token obtenido de Keycloak.

---

## Persistencia

### Configuración
Cada microservicio utiliza su propia **instancia H2 en memoria**:

| Servicio | String de conexión | Esquema | Características |
|---|---|---|---|
| `clase-microservice` | `jdbc:h2:mem:gimnasiodb` | `CLASE` (auto-creado) | Ephemeral, resiembra en boot |
| `entrenador-microservice` | `jdbc:h2:mem:gimnasiodb` | `ENTRENADOR` (auto-creado) | Ephemeral, resiembra en boot |
| `equipo-microservice` | `jdbc:h2:mem:gimnasiodb` | `EQUIPO` (auto-creado) | Ephemeral, resiembra en boot |
| `miembro-microservice` | `jdbc:h2:mem:gimnasiodb` | `MIEMBRO` (auto-creado) | Ephemeral, resiembra en boot |
| `pago-microservice` | `jdbc:h2:mem:gimnasiodb` | `PAGO` (auto-creado) | Ephemeral, sin datos semilla |

**Notas importantes:**
- Aunque todas usan el mismo nombre de BD (`gimnasiodb`), son **instancias separadas e independientes** — cada JVM de Spring Boot levanta su propio H2 en memoria, sin compartir datos reales
- Los datos **se pierden al reiniciar** cada servicio
- Cada servicio tiene un `DataLoader.java` que resiembra datos de prueba en el arranque (`@Component` + `CommandLineRunner`)
- **No hay persistencia externa** (PostgreSQL, MySQL, etc.)

### Modelos de Datos (DDD)
```
Clase
├── id (PK)
├── nombre
├── horario
├── capacidad
└── entrenadorId (FK referencia lógica)

Entrenador
├── id (PK)
├── nombre
├── especialidad
└── salario

Equipo
├── id (PK)
├── nombre
├── tipo
├── estado
└── ultimoMantenimiento

Miembro
├── id (PK)
├── nombre
├── email
├── membresiaActiva
└── fechaRegistro

Pago
├── id (PK)
├── miembroId (referencia lógica)
├── monto
├── metodoPago
├── estado (PENDIENTE | APROBADO | FALLIDO)
├── intentos
├── motivoFallo
├── fechaSolicitud
└── fechaProcesamiento
```

---

## Endpoints REST

### Clase Service (8080)
```
POST   /api/gimnasio/clases
       Request: { "nombre": "Yoga", "horario": "10:00", "capacidad": 20, "entrenadorId": 1 }
       Response: { "id": 1, "nombre": "Yoga", ... }

GET    /api/gimnasio/clases
       Response: [
         { "id": 1, "nombre": "Yoga", ..., "entrenador": { "id": 1, "nombre": "Juan", ... } },
         ...
       ]
```

### Entrenador Service (8081)
```
POST   /api/gimnasio/entrenadores
       Request: { "nombre": "Juan Pérez", "especialidad": "Cardio", "salario": 2500 }
       Response: { "id": 1, ... }

GET    /api/gimnasio/entrenadores
       Response: [ { "id": 1, "nombre": "Juan Pérez", ... }, ... ]

GET    /api/gimnasio/entrenadores/{id}
       Response: { "id": 1, "nombre": "Juan Pérez", "especialidad": "Cardio", "salario": 2500 }
```

### Equipo Service (8082)
```
POST   /api/gimnasio/equipos
       Request: { "nombre": "Treadmill", "tipo": "Cardio", "estado": "Operativo", ... }
       Response: { "id": 1, ... }

GET    /api/gimnasio/equipos
       Response: [ { "id": 1, "nombre": "Treadmill", ... }, ... ]
```

### Miembro Service (8083)
```
POST   /api/gimnasio/miembros
       Request: { "nombre": "Carlos López", "email": "carlos@mail.com", "membresiaActiva": true, ... }
       Response: { "id": 1, ... }

GET    /api/gimnasio/miembros
       Response: [ { "id": 1, "nombre": "Carlos López", ... }, ... ]
```

### Pago Service (8086)
```
POST   /api/gimnasio/pagos
       Request: { "miembroId": 1, "monto": 120000, "metodoPago": "TARJETA" }
       Response (202): { "id": 1, "estado": "PENDIENTE", "intentos": 0, ... }

GET    /api/gimnasio/pagos
       Response: [ { "id": 1, "estado": "APROBADO", "intentos": 1, "motivoFallo": null, ... }, ... ]

GET    /api/gimnasio/pagos/fallidos
       Response: [ { "id": 2, "estado": "FALLIDO", "intentos": 3, "motivoFallo": "La pasarela rechazó ...", ... } ]
```

---

## Cómo Ejecutar el Sistema

### Prerequisitos
- Java 17 (o compatible con Spring Boot 3.3.2)
- Maven 3.8+
- Docker y Docker Compose

### Con Docker Compose (recomendado)

```bash
docker-compose up --build
```

Servicios disponibles:
- **Keycloak**: `http://localhost:8180` (admin: admin / admin)
- **API Gateway**: `http://localhost:8080`
- **RabbitMQ Dashboard**: `http://localhost:15672` (guest / guest)
- **Swagger UI** (ejemplo): `http://localhost:8081/swagger-ui.html`

### Obtener Token para Pruebas
```bash
curl -X POST http://localhost:8180/realms/gimnasio/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gimnasio-gateway" \
  -d "username=admin" \
  -d "password=admin123"
```

### Sin Docker, cada servicio suelto

1. Arrancar RabbitMQ y Keycloak localmente.
2. Compilar y arrancar:
```bash
./mvnw compile              # compila todos los módulos desde la raíz
cd services/entrenador-microservice && ./mvnw spring-boot:run
cd services/clase-microservice && ./mvnw spring-boot:run
cd services/equipo-microservice && ./mvnw spring-boot:run
cd services/miembro-microservice && ./mvnw spring-boot:run
cd services/notificacion-microservice && ./mvnw spring-boot:run
cd services/pago-microservice && ./mvnw spring-boot:run
cd services/api-gateway && ./mvnw spring-boot:run
```

### Validación
```bash
# Probar un endpoint con token
TOKEN=$(curl -s -X POST http://localhost:8180/realms/gimnasio/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=gimnasio-gateway" \
  -d "username=admin" -d "password=admin123" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/entrenadores
```

---

## Limitaciones Conocidas

| Limitación | Impacto | Mitigation |
|---|---|---|
| **Sin Service Discovery** | URLs de servicios fijadas por variables de entorno; no escalable horizontalmente | Integrar Eureka o Service Registry |
| **Gateway manual** | Cada ruta nueva requiere un controlador con `RestClient` en `api-gateway` | Migrar a Spring Cloud Gateway |
| **DTOs de eventos duplicados** | `MiembroInscritoEvent` y `EquipoAveriadoEvent` se copian en productor y consumidor; deben mantenerse en el mismo paquete porque el `__TypeId__` usa el nombre completo de la clase | Módulo compartido de contratos o mapeo de tipos explícito |
| **H2 en memoria** | Datos se pierden en restart; no persistencia | Migrar a PostgreSQL/MySQL + volúmenes persistentes |
| **Sin tests unitarios/integración** | Solo smoke test de contexto Spring | Escribir unit tests (Mockito) e IT con testcontainers |
| **Healthcheck de Keycloak** | El healthcheck del compose usa `/dev/tcp` y apunta a `/health/ready`; puede no funcionar si la imagen no tiene bash o el endpoint no está en puerto 8080 | Verificar al ejecutar `docker compose up`; fallback: cambiar `condition: service_healthy` → `service_started` |
| **Diagrama .drawio vacío** | Entregable pendiente | Este archivo `.md` complementa/reemplaza ese diagrama |


---

## Referencias

- **Proyecto académico**: `doc/project.md` (assignment brief en español)
- **Código base**: Cada servicio en su directorio: `services/{clase,entrenador,equipo,miembro,notificacion,pago}-microservice/` y `services/api-gateway/`
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Spring Cloud**: https://spring.io/projects/spring-cloud
- **Domain-Driven Design**: Evans, E. "Domain-Driven Design: Tackling Complexity in the Heart of Software"

---

**Fecha**: 2026-08-28  
**Autor**: Equipo de Desarrollo (e5jparra1)
