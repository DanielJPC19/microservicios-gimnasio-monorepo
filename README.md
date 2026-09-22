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
| `monitoreo-microservice`  | 8087   | Monitoreo en tiempo real y análisis de entrenamientos (Kafka) |
| `RabbitMQ`                | 5672 / 15672 | Message Broker & Dashboard UI |
| `Kafka` (KRaft)           | 29092 (host) / 9092 (red interna) | Streaming de eventos |
| `Keycloak`                | 8180   | Proveedor de identidad (IAM), autenticación JWT |

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
- **Streaming (Kafka)**:
  1. **Ocupación de clases en tiempo real**: `clase-microservice` publica `OcupacionClase` en `ocupacion-clases` (key = `claseId`) en cada ingreso/salida. `monitoreo-microservice` (grupo `monitoreo-grupo`) actualiza el dashboard y lo emite por SSE.
  2. **Análisis de entrenamientos (Kafka Streams)**: `miembro-microservice` publica `DatosEntrenamiento` en `datos-entrenamiento` (key = `miembroId`). Un stream processor agrega por miembro en ventanas de 7 días (`resumen-entrenamiento-store`) y publica en `resumen-entrenamiento`.
  3. **Recuperación ante fallos**: un consumidor propio (`recuperacion-grupo`) procesa ambos topics guardando el resultado y el offset en la misma transacción (H2 en archivo). Al reiniciar hace `seek` al último checkpoint.

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
  monitoreo-microservice/
doc/
keycloak/realm/        # realm JSON para importación automática en Keycloak
pom.xml                # aggregator, compila los módulos
docker-compose.yml     # orquesta los 8 servicios + rabbitmq + keycloak + kafka
```

## Cómo correr

**Con Docker Compose (recomendado para demo):**

```bash
docker-compose up --build
```
> El panel web de administración de RabbitMQ queda disponible en: `http://localhost:15672` (usuario: `guest`, contraseña: `guest`).
> Keycloak queda disponible en: `http://localhost:8180` (usuario admin: `admin`, contraseña: `admin`). El realm `gimnasio` se importa automáticamente.

**Obtener token JWT para pruebas:**
```bash
curl -X POST http://localhost:8180/realms/gimnasio/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=gimnasio-gateway" \
  -d "username=admin" \
  -d "password=admin123"
```
> Usar el `access_token` del response como header: `Authorization: Bearer <token>`.

**Usuarios de prueba:**

| Usuario | Contraseña | Roles |
|---------|-----------|-------|
| `admin` | `admin123` | ROLE_ADMIN |
| `entrenador1` | `trainer123` | ROLE_TRAINER |
| `miembro1` | `member123` | ROLE_MEMBER |

**Sin Docker, cada servicio suelto:**

1. Levantar RabbitMQ y Keycloak localmente.
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

## Swagger / OpenAPI

Cada microservicio expone documentación interactiva:

| Servicio | Swagger UI |
|----------|-----------|
| API Gateway | `http://localhost:8080/swagger-ui.html` |
| Entrenador | `http://localhost:8081/swagger-ui.html` |
| Equipo | `http://localhost:8082/swagger-ui.html` |
| Miembro | `http://localhost:8083/swagger-ui.html` |
| Clase | `http://localhost:8084/swagger-ui.html` |
| Pago | `http://localhost:8086/swagger-ui.html` |
| Monitoreo | `http://localhost:8087/swagger-ui.html` |

> Los endpoints protegidos requieren un token JWT. En Swagger UI, clic en "Authorize" y pegar el token obtenido de Keycloak.

## Probar Flujos Asincrónicos (RabbitMQ)

> Todos los endpoints (excepto `GET /api/gimnasio`) requieren autenticación JWT.
> Agregar el header `Authorization: Bearer <token>` a cada request, o usar Swagger UI con "Authorize".

### 1. Inscripción de Miembro (Direct Exchange -> Notificación de bienvenida)
```bash
# Obtener token (reutilizar TOKEN en todos los ejemplos)
TOKEN=$(curl -s -X POST http://localhost:8180/realms/gimnasio/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=gimnasio-gateway" \
  -d "username=admin" -d "password=admin123" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

curl -X POST http://localhost:8080/api/gimnasio/miembros \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Carlos Mendoza", "email": {"email": "carlos@gmail.com"}, "fechaInscripcion": {"fechaInscripcion": "2026-09-14"}}'
```
*Ver en la consola de `notificacion-service` el log con el envío del correo de bienvenida.*

### 2. Reporte de Avería de Equipo (Fanout Exchange Pub/Sub -> 3 colas en simultáneo)
```bash
curl -X POST http://localhost:8080/api/gimnasio/equipos/1/reportar-averia \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"motivo": "Fallo en motor de tracción y banda rota", "gravedad": "ALTA"}'
```
*Ver en la consola de `notificacion-service` los 3 logs procesados simultáneamente (Mantenimiento Técnico, Alerta Entrenadores y Push App Socios).*

### 3. Pagos y Dead Letter Queue
```bash
# Aprobado: 1 intento
curl -X POST http://localhost:8080/api/gimnasio/pagos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"miembroId": 1, "monto": 120000, "metodoPago": "TARJETA"}'

# Rechazado por la pasarela: 3 intentos (1s, 2s) y luego a la DLQ
curl -X POST http://localhost:8080/api/gimnasio/pagos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"miembroId": 1, "monto": 120000, "metodoPago": "TARJETA_RECHAZADA"}'

# Inválido: sin reintentos, directo a la DLQ
curl -X POST http://localhost:8080/api/gimnasio/pagos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"miembroId": 1, "monto": -5000, "metodoPago": "EFECTIVO"}'

# Consultar estado (solo ADMIN)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/pagos
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/pagos/fallidos
```
*En la consola de `pago-service` se ven los intentos, el `Retries exhausted` y el log `[DEAD LETTER QUEUE]` con la cabecera `x-death` (cola de origen y razón `rejected`).*

**Ver los mensajes dentro de la DLQ en el panel de RabbitMQ:** por defecto el consumidor de la DLQ los procesa al instante. Para que se queden en la cola, levantar `pago-service` con el consumidor pausado:
```bash
PAGO_DLQ_CONSUMIDOR_ACTIVO=false docker-compose up -d pago-service
```
Luego en `http://localhost:15672` → *Queues* → `pagos.procesamiento.dlq` → *Get messages*. Los pagos quedan en `PENDIENTE` con `intentos` y `motivoFallo` registrados. Al volver a `true` el consumidor vacía la DLQ (el reinicio borra la base H2 en memoria, así que esos pagos ya no aparecen en `/pagos`).

## Probar Streaming (Kafka)

### 1. Ocupación de clases en tiempo real
```bash
# Terminal 1: dashboard en vivo (SSE directo a monitoreo-service; el gateway no reenvía streams)
curl -N -H "Authorization: Bearer $TOKEN" http://localhost:8087/api/gimnasio/monitoreo/ocupacion/stream

# Terminal 2: generar movimiento en la clase 1
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/clases/1/ingreso
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/clases/1/ingreso
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/clases/1/salida

# Foto actual del dashboard
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/monitoreo/ocupacion
```
*La terminal 1 recibe un evento `ocupacion` por cada cambio. En la consola de `monitoreo-service` aparece el log `[DASHBOARD]`.*

### 2. Análisis de entrenamientos (Kafka Streams)
```bash
curl -X POST http://localhost:8080/api/gimnasio/miembros/1/entrenamientos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo": "CARDIO", "duracionMinutos": 45, "calorias": 400}'
curl -X POST http://localhost:8080/api/gimnasio/miembros/1/entrenamientos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo": "FUERZA", "duracionMinutos": 60, "calorias": 350}'

# Resumen semanal (consulta interactiva al state store); esperar ~1s al commit de Streams
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/monitoreo/entrenamiento/1/resumen

# Ver el topic de salida
docker exec gimnasio-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic resumen-entrenamiento --from-beginning --property print.key=true
```

### 3. Recuperación ante fallos (checkpoints)
```bash
# 1. Estado inicial: checkpoints por partición y eventos procesados (solo ADMIN)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/monitoreo/recuperacion/estado

# 2. Simular caída del consumidor
docker compose stop monitoreo-service

# 3. Mientras está caído, seguir produciendo (quedan retenidos en el log de Kafka)
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/gimnasio/clases/2/ingreso
curl -X POST http://localhost:8080/api/gimnasio/miembros/1/entrenamientos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo": "YOGA", "duracionMinutos": 30, "calorias": 150}'

# 4. Reiniciar: retoma desde el último checkpoint, sin perder ni duplicar eventos
docker compose start monitoreo-service
docker compose logs monitoreo-service | grep RECUPERACION
```
*Se ven los logs `Reanudando ocupacion-clases-N desde el offset X` y luego `Procesado ...` solo para los eventos nuevos. El checkpoint vive en H2 en archivo, en el volumen `monitoreo-data`.*

> [!note] Postman
> - También puede abrir en Postman la colección actualizada en `gimnasio.postman_collection.json`.
> - Establezca la variable `api_url` en: `http://localhost:8080/api/gimnasio`.

