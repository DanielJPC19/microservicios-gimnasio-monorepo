# Sistema Distribuido de Gestión de Gimnasio
## Arquitectura de Microservicios con Spring Boot, Keycloak, RabbitMQ y Apache Kafka

- **Ecosistema contenerizado**: 11 contenedores en Docker Compose (8 microservicios + 3 brokers/IdP)
- **Seguridad Zero-Trust**: OAuth2 / OpenID Connect y JWT con Keycloak 24
- **Mensajería Asíncrona**: RabbitMQ 3.13 (Direct Exchange, Fanout Pub/Sub y Dead Letter Queue)
- **Streaming en Tiempo Real**: Apache Kafka 3.8 (KRaft), Kafka Streams y Checkpoints de recuperación

---

# Arquitectura General: 8 Microservicios del Dominio

- **API Gateway (`:8080`)**: Punto único de entrada, validación perimetral de JWT y enrutamiento síncrono.
- **Entrenador Service (`:8081`)**: Catálogo de instructores y especialidades con base de datos H2.
- **Equipo Service (`:8082`)**: Inventario de máquinas y productor de alertas de avería en RabbitMQ.
- **Miembro Service (`:8083`)**: Registro de socios (RabbitMQ Direct) y telemetría de entrenamientos (Kafka).
- **Clase Service (`:8084`)**: Horarios de clases (RabbitMQ Fanout), enriquecimiento REST y control de aforo (Kafka).
- **Notificación Service (`:8085`)**: Consumidor centralizado de eventos en RabbitMQ (Email, Push Móvil y Auditoría).
- **Pago Service (`:8086`)**: Pasarela de cobros asíncrona con reintentos exponenciales y Dead Letter Queue (DLQ).
- **Monitoreo Service (`:8087`)**: Dashboard en vivo por SSE, agregación semanal con Kafka Streams y control de offsets.

---

# Parte 1: Comunicación Síncrona (REST) y Documentación OpenAPI

- **Integración Síncrona (`clase-service` → `entrenador-service`)**:
  - Al consultar `GET /api/gimnasio/clases`, `clase-service` invoca síncronamente a `entrenador-service` propagando el token `Authorization: Bearer <jwt>`.
  - Combina en tiempo real la información de la clase con el nombre y especialidad del instructor asignado.
- **Documentación Interactiva con Swagger / OpenAPI 3.0**:
  - Los 7 microservicios HTTP exponen `/swagger-ui.html` y `/v3/api-docs`.
  - Esquema de seguridad `BearerAuth` integrado para probar endpoints protegidos directamente desde el navegador.

---

# Parte 1: Seguridad Centralizada con Keycloak (OAuth2 + JWT)

- **Realm `gimnasio` en Keycloak 24 (`:8180`)**:
  - Configuración exportada e importada automáticamente al iniciar el contenedor (`gimnasio-realm.json`).
  - Clientes confidenciales por microservicio y cliente público `gimnasio-gateway`.
- **Control de Acceso Basado en Roles (RBAC)**:
  - **ROLE_ADMIN**: Control total (creación de entrenadores/equipos, auditoría de DLQ y checkpoints de Kafka).
  - **ROLE_TRAINER**: Gestión de clases, reprogramación de horarios y reporte de averías de máquinas.
  - **ROLE_MEMBER**: Inscripción, registro de sesiones de entrenamiento, ingreso a clases y pagos.
- **Doble Validación (`Defense in Depth`)**:
  - Validación de firma JWT contra JWK Set URI tanto en el **API Gateway** como en cada **Microservicio** con `@PreAuthorize`.

---

# Parte 2: RabbitMQ — Notificación Asíncrona (Direct Exchange)

- **Caso de Uso: Inscripción de Nuevos Socios (`POST /api/gimnasio/miembros`)**:
  - Desacopla el registro en base de datos del envío del correo de bienvenida.
- **Flujo en RabbitMQ**:
  1. `miembro-service` guarda al socio en H2 y publica `MiembroInscritoEvent`.
  2. El evento llega al `DirectExchange` **`gimnasio.miembro.exchange`** con routing key `miembro.inscrito`.
  3. Se encola de forma durable en **`miembro.inscripcion.notificacion`**.
  4. `notificacion-service` consume el mensaje asíncronamente y despacha el correo de bienvenida.

---

# Parte 2: RabbitMQ — Patrón Publish/Subscribe (Fanout Exchange)

- **Difusión Simultánea (Broadcast) a Múltiples Consumidores**:
  - Un `FanoutExchange` replica cada evento en todas las colas suscritas simultáneamente.
- **Flujo 1: Cambio de Horario de Clases (`PUT /api/gimnasio/clases/{id}/horario`)**:
  - Exchange **`gimnasio.clase.horario.events`** distribuye `HorarioClaseCambiadoEvent` a 3 colas en paralelo:
  - **Cola App Móvil (`clase.horario.app-movil`)**: Notificación Push a los socios inscritos.
  - **Cola Email (`clase.horario.email`)**: Correo electrónico con actualización de calendario (`.ics`).
  - **Cola Auditoría (`clase.horario.auditoria`)**: Registro en bitácora operativa del gimnasio.
- **Flujo 2: Reporte de Avería de Equipos (`POST /api/gimnasio/equipos/{id}/reportar-averia`)**:
  - Exchange **`gimnasio.equipo.events`** alerta simultáneamente a **Mantenimiento**, **Entrenadores** y **App de Socios**.

---

# Parte 2: RabbitMQ — Manejo de Errores y Dead Letter Queue (DLQ)

- **Procesamiento Asíncrono de Pagos (`POST /api/gimnasio/pagos`)**:
  - Registra el pago en estado `PENDIENTE`, publica `PagoSolicitadoEvent` en `pagos.procesamiento` y responde `202 Accepted`.
- **Estrategia de Reintentos con Backoff Exponencial**:
  - **Fallo de pasarela (`TARJETA_RECHAZADA`)**: Reintenta automáticamente **3 veces** con esperas de `1s` y `2s`.
  - **Datos inválidos (monto negativo)**: Falla inmediatamente sin reintentar.
- **Enrutamiento a la Dead Letter Queue (DLQ)**:
  - Al agotar los 3 intentos, `RejectAndDontRequeueRecoverer` rechaza el mensaje sin reencolar.
  - RabbitMQ lo desvía al **Dead Letter Exchange (`gimnasio.pagos.dlx`)** → cola **`pagos.procesamiento.dlq`**.
  - El consumidor DLQ extrae el header `x-death`, marca el pago como **`FALLIDO`** y lo expone en `GET /api/gimnasio/pagos/fallidos`.

---

# Parte 3: Apache Kafka — Ocupación de Clases en Tiempo Real

- **Telemetría de Aforo en Vivo (`ocupacion-clases`)**:
  - Cada ingreso o salida (`POST /clases/{id}/ingreso` y `/salida`) actualiza el cupo con bloqueo pesimista y publica `OcupacionClase` en Apache Kafka (modo KRaft, 3 particiones, retención de 7 días).
- **Dashboard Reactivo con Server-Sent Events (SSE)**:
  - `monitoreo-service` consume el stream en tiempo real, calcula el porcentaje de ocupación (`DISPONIBLE`, `CASI_LLENA`, `LLENA`) y lo transmite en vivo por `GET /api/gimnasio/monitoreo/ocupacion/stream`.
  - Al arrancar, reconstruye el estado completo leyendo desde el inicio del log retenido (`seekToBeginning`).

---

# Parte 3: Kafka Streams (Ventanas 7d) y Recuperación de Offsets

- **Análisis Semanal de Entrenamientos con Kafka Streams**:
  - `miembro-service` publica cada rutina (tipo, minutos, calorías) en el topic **`datos-entrenamiento`**.
  - `KafkaStreamsConfig` agrupa por socio en una **ventana temporal de 7 días (`TimeWindows`)**, materializa el acumulado en el State Store **`resumen-entrenamiento-store`** y publica en **`resumen-entrenamiento`**.
  - Consulta inmediata mediante Interactive Queries en `GET /monitoreo/entrenamiento/{id}/resumen`.
- **Tolerancia a Fallos con Checkpoints Transaccionales (`RecuperacionService`)**:
  - Consumidor con `enable.auto.commit=false` respaldado por H2 persistente en disco (`/data/monitoreo-db`).
  - Guarda cada evento procesado y su `ultimoOffset` en la misma transacción; al reiniciar tras un fallo, retoma exactamente desde **`ultimoOffset + 1`** (`consumer.seek`).

---

# Comparativa Arquitectónica y Demostración en Vivo

- **REST Síncrono**: Ideal para consultas inmediatas con enriquecimiento de datos (`clase-service` → `entrenador-service`).
- **RabbitMQ (AMQP)**: Ideal para comandos asíncronos, distribución Pub/Sub a múltiples colas y flujos transaccionales con reintentos y **Dead Letter Queue**.
- **Apache Kafka**: Ideal para flujos continuos de eventos de alto volumen, agregaciones en ventanas de tiempo (**Kafka Streams**) y reprocesamiento histórico mediante **offsets**.
- **Orden de la Demo en Vivo**:
  1. **Seguridad (Keycloak)**: Validación de tokens JWT y prueba de permisos `ADMIN` (`200 OK`) vs `MEMBER` (`403 Forbidden`).
  2. **RabbitMQ**: Inscripción (Direct), cambio de horario de clase (Fanout a 3 colas) y cobro fallido (3 reintentos → DLQ).
  3. **Apache Kafka**: Aforo en tiempo real, resumen semanal en Kafka Streams y reinicio de contenedor recuperando offsets.
