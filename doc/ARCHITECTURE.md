# Arquitectura: Microservicios Gimnasio

## Resumen

Refactorización de un monolito Spring Boot ("Gimnasio") en microservicios independientes bajo principios de **Domain-Driven Design (DDD)**: 5 servicios de negocio (clase, entrenador, equipo, miembro, pago), un servicio consumidor de eventos (notificacion) y un API Gateway como punto de entrada único. La comunicación es síncrona vía REST (gateway → servicios, clase → entrenador) y asíncrona vía RabbitMQ (eventos de inscripción, averías y procesamiento de pagos con Dead Letter Queue). Persistencia mediante H2 en memoria, aislada por servicio.

**Stack tecnológico:**
- **Runtime**: Java 17 (Spring Boot 3.3.2)
- **Build**: Maven (mvnw)
- **Persistencia**: H2 (JDBC, JPA)
- **API**: REST (Spring Web)
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

---

## Diagrama de Componentes y Mensajería Asincrónica

```mermaid
graph TB
    subgraph "Clientes Externos"
        CLIENT["Cliente / Postman / Frontend"]
    end

    subgraph "API Gateway"
        GATEWAY["API Gateway (8080)"]
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

    CLIENT --> GATEWAY
    GATEWAY --> CLASE
    GATEWAY --> ENTRENADOR
    GATEWAY --> EQUIPO
    GATEWAY --> MIEMBRO
    GATEWAY --> PAGO

    CLASE -->|REST GET /entrenadores/{id}| ENTRENADOR

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
- Java 21 (o compatible con Spring Boot 3.3.2)
- Maven 3.8+

### Pasos de Arranque

**1. Arrancar Entrenador Service primero** (dependerá hacia él):
```bash
cd services/entrenador-microservice
./mvnw spring-boot:run
# Esperará en http://localhost:8081
```

**2. En otra terminal, arrancar Clase Service** (depende de Entrenador):
```bash
cd services/clase-microservice
./mvnw spring-boot:run
# Esperará en http://localhost:8080
```

**3. En otra terminal, arrancar Equipo Service** (standalone):
```bash
cd services/equipo-microservice
./mvnw spring-boot:run
# Esperará en http://localhost:8082
```

**4. En otra terminal, arrancar Miembro Service** (standalone):
```bash
cd services/miembro-microservice
./mvnw spring-boot:run
# Esperará en http://localhost:8083
```

### Validación
```bash
# Cada servicio expone H2 Console (opcional)
curl http://localhost:8080/h2-console   # Clase
curl http://localhost:8081/h2-console   # Entrenador
curl http://localhost:8082/h2-console   # Equipo
curl http://localhost:8083/h2-console   # Miembro

# Probar un endpoint
curl http://localhost:8080/api/gimnasio/clases
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
