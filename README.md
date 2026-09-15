# Gimnasio - Arquitectura de Microservicios

Refactorización de un monolito Spring Boot de gestión de gimnasio en 4 microservicios independientes aplicando Domain-Driven Design (DDD). Taller académico — ver [`doc/project.md`](doc/project.md) para el enunciado completo.

## Equipo

1. Daniel Jose Plazas Cortes - A00400085
2. Rony Farid Ordoñez García - A00397968
3. Daniel Gonzales Rivera - A00399873
4. Juan Pablo Parra Betancourt - A00

## Arquitectura

| Servicio                  | Puerto | Dominio                 |
| ------------------------- | ------ | ----------------------- |
| `api-gateway`             | 8080   | API Gateway             |
| `clase-microservice`      | 8084   | Programación de clases  |
| `entrenador-microservice` | 8081   | Gestión de entrenadores |
| `equipo-microservice`     | 8082   | Inventario y averías    |
| `miembro-microservice`    | 8083   | Gestión de miembros     |
| `notificacion-microservice`| 8085  | Consumidor de eventos / Notificaciones |
| `RabbitMQ`                | 5672 / 15672 | Message Broker & Dashboard UI |

### Comunicación entre Servicios
- **Síncrona (REST)**: `clase-service` consulta a `entrenador-service` para enriquecer la respuesta de clases con los datos del instructor asignado. `api-gateway` rutea las peticiones externas.
- **Asíncrona (RabbitMQ)**:
  1. **Notificación de Inscripción (Direct Exchange)**: `miembro-microservice` emite `MiembroInscritoEvent` a `gimnasio.miembro.exchange` con routing key `miembro.inscrito`. `notificacion-microservice` procesa el evento y envía el correo de bienvenida.
  2. **Difusión de Averías de Equipos (Fanout Exchange - Pub/Sub)**: Cuando se reporta una máquina averiada en `equipo-microservice`, se publica `EquipoAveriadoEvent` a `gimnasio.equipo.events`. RabbitMQ difunde el mensaje en simultáneo a 3 colas de `notificacion-microservice`:
     - `equipo.averia.mantenimiento`: Genera ticket de reparación urgente para servicio técnico.
     - `equipo.averia.entrenadores`: Alerta a los instructores de sala para reprogramar rutinas.
     - `equipo.averia.app-socios`: Dispara notificación push a la app móvil de los socios.

Detalle completo (modelos, endpoints, decisiones y limitaciones conocidas): [`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md). Diagrama de despliegue: [`doc/DeploymentDiagram.drawio`](doc/DeploymentDiagram.drawio).

## Estructura del repo

```
services/
  api-gateway/
  clase-microservice/
  entrenador-microservice/
  equipo-microservice/
  miembro-microservice/
  notificacion-microservice/
doc/
pom.xml            # aggregator, compila los módulos
docker-compose.yml # orquesta los 6 microservicios + rabbitmq
```

## Cómo correr

**Con Docker Compose (recomendado para demo):**

```bash
docker-compose up --build
```
> El panel web de administración de RabbitMQ queda disponible en: `http://localhost:15672` (usuario: `guest`, contraseña: `guest`).

**Sin Docker, cada servicio suelto:**

1. Levantar RabbitMQ localmente (o vía `docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management`).
2. Compilar y arrancar:
```bash
./mvnw compile              # compila todos los módulos desde la raíz
cd services/entrenador-microservice && ./mvnw spring-boot:run
cd services/clase-microservice && ./mvnw spring-boot:run
cd services/equipo-microservice && ./mvnw spring-boot:run
cd services/miembro-microservice && ./mvnw spring-boot:run
cd services/notificacion-microservice && ./mvnw spring-boot:run
cd services/api-gateway && ./mvnw spring-boot:run
```

## Probar Flujos Asincrónicos (RabbitMQ)

### 1. Inscripción de Miembro (Direct Exchange -> Notificación de bienvenida)
```bash
curl -X POST http://localhost:8080/api/gimnasio/miembros \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Carlos Mendoza", "email": {"email": "carlos@gmail.com"}, "fechaInscripcion": {"fechaInscripcion": "2026-09-14"}}'
```
*Ver en la consola de `notificacion-service` el log con el envío del correo de bienvenida.*

### 2. Reporte de Avería de Equipo (Fanout Exchange Pub/Sub -> 3 colas en simultáneo)
```bash
curl -X POST http://localhost:8080/api/gimnasio/equipos/1/reportar-averia \
  -H "Content-Type: application/json" \
  -d '{"motivo": "Fallo en motor de tracción y banda rota", "gravedad": "ALTA"}'
```
*Ver en la consola de `notificacion-service` los 3 logs procesados simultáneamente (Mantenimiento Técnico, Alerta Entrenadores y Push App Socios).*

> [!note] Postman
> - También puede abrir en Postman la colección actualizada en `gimnasio.postman_collection.json`.
> - Establezca la variable `api_url` en: `http://localhost:8080/api/gimnasio`.

