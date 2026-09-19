# Gimnasio - Arquitectura de Microservicios

Refactorización de un monolito Spring Boot de gestión de gimnasio en microservicios independientes aplicando Domain-Driven Design (DDD). Taller académico — ver [`doc/project.md`](doc/project.md) para el enunciado completo.

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
| `pago-microservice`       | 8086   | Procesamiento de pagos (con Dead Letter Queue) |
| `RabbitMQ`                | 5672 / 15672 | Message Broker & Dashboard UI |

### Comunicación entre Servicios
- **Síncrona (REST)**: `clase-service` consulta a `entrenador-service` para enriquecer la respuesta de clases con los datos del instructor asignado. `api-gateway` rutea las peticiones externas.
- **Asíncrona (RabbitMQ)**:
  1. **Notificación de Inscripción (Direct Exchange)**: `miembro-microservice` emite `MiembroInscritoEvent` a `gimnasio.miembro.exchange` con routing key `miembro.inscrito`. `notificacion-microservice` procesa el evento y envía el correo de bienvenida.
  2. **Difusión de Averías de Equipos (Fanout Exchange - Pub/Sub)**: Cuando se reporta una máquina averiada en `equipo-microservice`, se publica `EquipoAveriadoEvent` a `gimnasio.equipo.events`. RabbitMQ difunde el mensaje en simultáneo a 3 colas de `notificacion-microservice`:
     - `equipo.averia.mantenimiento`: Genera ticket de reparación urgente para servicio técnico.
     - `equipo.averia.entrenadores`: Alerta a los instructores de sala para reprogramar rutinas.
     - `equipo.averia.app-socios`: Dispara notificación push a la app móvil de los socios.
  3. **Pagos con Dead Letter Queue**: `pago-microservice` recibe el pago (`202 Accepted`, estado `PENDIENTE`) y publica `PagoSolicitadoEvent` a `gimnasio.pagos.exchange` (routing key `pago.procesar`). El consumidor lo procesa contra una pasarela simulada:
     - Error de pasarela (`PagoRechazadoException`): hasta 3 intentos con backoff exponencial (1s, 2s).
     - Datos inválidos (`PagoInvalidoException`): no se reintenta.
     - Si falla definitivamente, el mensaje se rechaza sin reencolar y RabbitMQ lo desvía (`x-dead-letter-exchange`) a `gimnasio.pagos.dlx` → `pagos.procesamiento.dlq`, cuyo consumidor marca el pago como `FALLIDO`.

Detalle completo (modelos, endpoints, decisiones y limitaciones conocidas): [`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md). Diagrama de despliegue: [`doc/DeploymentDiagram.pdf`](doc/DeploymentDiagram.pdf).

## Estructura del repo

```
services/
  api-gateway/
  clase-microservice/
  entrenador-microservice/
  equipo-microservice/
  miembro-microservice/
  notificacion-microservice/
  pago-microservice/
doc/
pom.xml            # aggregator, compila los módulos
docker-compose.yml # orquesta los 7 servicios + rabbitmq
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
cd services/pago-microservice && ./mvnw spring-boot:run
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

### 3. Pagos y Dead Letter Queue
```bash
# Aprobado: 1 intento
curl -X POST http://localhost:8080/api/gimnasio/pagos \
  -H "Content-Type: application/json" \
  -d '{"miembroId": 1, "monto": 120000, "metodoPago": "TARJETA"}'

# Rechazado por la pasarela: 3 intentos (1s, 2s) y luego a la DLQ
curl -X POST http://localhost:8080/api/gimnasio/pagos \
  -H "Content-Type: application/json" \
  -d '{"miembroId": 1, "monto": 120000, "metodoPago": "TARJETA_RECHAZADA"}'

# Inválido: sin reintentos, directo a la DLQ
curl -X POST http://localhost:8080/api/gimnasio/pagos \
  -H "Content-Type: application/json" \
  -d '{"miembroId": 1, "monto": -5000, "metodoPago": "EFECTIVO"}'

# Consultar estado
curl http://localhost:8080/api/gimnasio/pagos           # todos, con estado e intentos
curl http://localhost:8080/api/gimnasio/pagos/fallidos  # los que terminaron en la DLQ
```
*En la consola de `pago-service` se ven los intentos, el `Retries exhausted` y el log `[DEAD LETTER QUEUE]` con la cabecera `x-death` (cola de origen y razón `rejected`).*

**Ver los mensajes dentro de la DLQ en el panel de RabbitMQ:** por defecto el consumidor de la DLQ los procesa al instante. Para que se queden en la cola, levantar `pago-service` con el consumidor pausado:
```bash
PAGO_DLQ_CONSUMIDOR_ACTIVO=false docker-compose up -d pago-service
```
Luego en `http://localhost:15672` → *Queues* → `pagos.procesamiento.dlq` → *Get messages*. Los pagos quedan en `PENDIENTE` con `intentos` y `motivoFallo` registrados. Al volver a `true` el consumidor vacía la DLQ (el reinicio borra la base H2 en memoria, así que esos pagos ya no aparecen en `/pagos`).

> [!note] Postman
> - También puede abrir en Postman la colección actualizada en `gimnasio.postman_collection.json`.
> - Establezca la variable `api_url` en: `http://localhost:8080/api/gimnasio`.

