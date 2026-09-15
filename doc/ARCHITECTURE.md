# Arquitectura: Microservicios Gimnasio

## Resumen

Refactorización de un monolito Spring Boot ("Gimnasio") en 4 microservicios independientes bajo principios de **Domain-Driven Design (DDD)**. Cada servicio gestiona su propio dominio de negocio, con comunicación síncrona REST punto-a-punto (sin API Gateway, service discovery, ni message broker). Persistencia mediante H2 en memoria, aislada por servicio.

**Stack tecnológico:**
- **Runtime**: Java 21 (Spring Boot 3.3.2)
- **Build**: Maven (mvnw)
- **Persistencia**: H2 (JDBC, JPA)
- **API**: REST (Spring Web)
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
| **RabbitMQ** | 5672 / 15672 | Broker | Enrutamiento de colas y exchanges AMQP | Direct & Fanout Exchanges |

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
    end

    subgraph "RabbitMQ Broker (5672 / 15672)"
        EX_DIRECT["Direct Exchange:<br/>gimnasio.miembro.exchange"]
        Q_MIEMBRO["Cola: miembro.inscripcion.notificacion"]

        EX_FANOUT["Fanout Exchange:<br/>gimnasio.equipo.events"]
        Q_MANT["Cola: equipo.averia.mantenimiento"]
        Q_ENTR["Cola: equipo.averia.entrenadores"]
        Q_APP["Cola: equipo.averia.app-socios"]
    end

    subgraph "Microservicio Consumidor"
        NOTIF["Notificacion Service (8085)"]
    end

    CLIENT --> GATEWAY
    GATEWAY --> CLASE
    GATEWAY --> ENTRENADOR
    GATEWAY --> EQUIPO
    GATEWAY --> MIEMBRO

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
| **URL hardcoded en `ClaseService`** | Si `entrenador-microservice` cambia puerto o máquina, la integración se rompe | Mover a `application.properties` o usar Spring Cloud Config |
| **Sin Docker/docker-compose** | Arranque manual en 4 terminales; difícil de reproducir | Crear `Dockerfile` + `docker-compose.yml` |
| **Sin Service Discovery** | IPs/puertos hardcoded; no escalable | Integrar Eureka o Service Registry |
| **Sin API Gateway** | Clientes llaman directamente a cada servicio; sin punto de entrada único | Añadir Spring Cloud Gateway o Kong |
| **Sin Message Broker** | Integración síncrona = bloqueo en fallos de red | Considerar Kafka/RabbitMQ para async |
| **H2 en memoria** | Datos se pierden en restart; no persistencia | Migrar a PostgreSQL/MySQL + volúmenes persistentes |
| **Sin tests unitarios/integración** | Solo smoke test de contexto Spring | Escribir unit tests (Mockito) e IT con testcontainers |
| **Diagrama .drawio vacío** | Entregable pendiente | Este archivo `.md` complementa/reemplaza ese diagrama |


---

## Referencias

- **Proyecto académico**: `md/project.md` (assignment brief en español)
- **Código base**: Cada microservicio en su directorio: `{clase,entrenador,equipo,miembro}-microservice/`
- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **Spring Cloud**: https://spring.io/projects/spring-cloud
- **Domain-Driven Design**: Evans, E. "Domain-Driven Design: Tackling Complexity in the Heart of Software"

---

**Fecha**: 2026-08-28  
**Autor**: Equipo de Desarrollo (e5jparra1)
