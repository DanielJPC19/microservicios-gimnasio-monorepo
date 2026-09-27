# Guía Simple para Probar Todo en Postman (Seguridad, RabbitMQ, Kafka y DDD)

Esta guía explica paso a paso cómo probar cada parte del proyecto en **Postman**, qué resultado debes ver en pantalla y qué **URLs o comandos** abrir para comprobar visualmente que **RabbitMQ**, **Kafka** y **Keycloak** están funcionando.

---

## 1. URLs para tener abiertas en tu navegador

Antes de empezar, puedes abrir estas páginas en tu navegador mientras haces las pruebas:

| Herramienta | URL | Credenciales | ¿Qué ves ahí? |
|---|---|---|---|
| **RabbitMQ Panel Web** | [http://localhost:15672](http://localhost:15672) | Usuario: `guest`<br>Clave: `guest` | Pestañas **Exchanges** y **Queues** con gráficas en vivo de los mensajes publicados y consumidos. |
| **Keycloak Admin** | [http://localhost:8180](http://localhost:8180) | Usuario: `admin`<br>Clave: `admin` | El realm `gimnasio`, los usuarios (`admin`, `entrenador1`, `miembro1`) y sus roles. |
| **Swagger API Gateway** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | *(Usa el token Bearer)* | Todos los endpoints documentados en un solo lugar. |

---

## 2. Configuración inicial en Postman (1 minuto)

1. Abre **Postman** → haz clic en **Import** (arriba a la izquierda).
2. Selecciona estos 2 archivos de la raíz del proyecto:
   - `gimnasio.postman_collection.json`
   - `gimnasio.postman_environment.json`
3. Arriba a la derecha en Postman (donde dice *No Environment*), selecciona **`Gimnasio Local (Docker)`**.

### Paso Obligatorio antes de probar: Obtener los Tokens
> Los tokens de Keycloak vencen cada **5 minutos**. Si en algún momento te sale `401 Unauthorized`, vuelve a correr estos 3 logins.

Entra a la carpeta **`seguridad (Keycloak & RBAC)` → `01. Obtención de Tokens (AuthN)`**:
1. Abre **`1. Login Admin (ROLE_ADMIN)`** → dale **Send** (debe salir `200 OK`).
2. Abre **`2. Login Entrenador (ROLE_TRAINER)`** → dale **Send** (debe salir `200 OK`).
3. Abre **`3. Login Miembro (ROLE_MEMBER)`** → dale **Send** (debe salir `200 OK`).

*(Esto guarda automáticamente los tokens en las variables de entorno de Postman para todas las demás peticiones).*

---

## 3. Cómo probar RabbitMQ y ver que funcionó

Abre en tu navegador el panel de RabbitMQ: **[http://localhost:15672/#/queues](http://localhost:15672/#/queues)** (login: `guest` / `guest`).
Y si quieres ver los mensajes en consola en vivo, abre una terminal y corre:
```bash
docker compose logs -f notificacion-service pago-service
```

### A. Notificación de Inscripción (Direct Exchange)
1. **En Postman**: Ve a la carpeta **`miembros` → `miembros POST`** y dale **Send**.
2. **Qué debes ver en Postman**: Status **`200 OK`** con el miembro creado en formato JSON plano:
   ```json
   {
     "id": 3,
     "nombre": "Juan Perez",
     "email": "juan@email.com",
     "fechaInscripcion": "2026-08-31"
   }
   ```
3. **Cómo comprobarlo en RabbitMQ**:
   - En [http://localhost:15672/#/queues](http://localhost:15672/#/queues) busca la cola **`miembro.inscripcion.notificacion`**. Si haces clic sobre ella verás en la gráfica que entró y se procesó el mensaje.
   - En los logs de `notificacion-service` verás: `[NOTIFICACIÓN DE BIENVENIDA ENVIADA] -> Destinatario: Juan Perez <juan@email.com>`.

---

### B. Patrón Publish/Subscribe — Fanout Exchange (1 evento → 3 colas al mismo tiempo)
Tienes dos ejemplos listos para probar el Fanout:

1. **En Postman**:
   - Ve a **`clases` → `clases cambiar horario (Pub/Sub Fanout)`** y dale **Send** (`200 OK`).
   - Ve a **`equipos` → `equipos reportar averia (Pub/Sub Fanout)`** y dale **Send** (`200 OK`).
2. **Cómo comprobarlo en RabbitMQ**:
   - En [http://localhost:15672/#/exchanges](http://localhost:15672/#/exchanges) verás los exchanges tipo `fanout`:
     - **`gimnasio.clase.horario.events`**
     - **`gimnasio.equipo.events`**
   - En [http://localhost:15672/#/queues](http://localhost:15672/#/queues) verás que las **3 colas de clase** (`clase.horario.app-movil`, `clase.horario.email`, `clase.horario.auditoria`) y las **3 colas de equipo** (`equipo.averia.mantenimiento`, `equipo.averia.entrenadores`, `equipo.averia.app-socios`) recibieron y procesaron el mensaje simultáneamente.
   - En los logs de `notificacion-service` verás los 3 bloques impresos al instante (App Móvil, Email y Auditoría / Mantenimiento, Entrenadores y App Socios).

---

### C. Pagos Asíncronos y Dead Letter Queue (DLQ)
1. **En Postman** (carpeta **`pagos`**):
   - Abre **`pagos POST (aprobado)`** y dale **Send** → responde **`202 Accepted`** con `"estado": "PENDIENTE"`.
   - Abre **`pagos POST (rechazado: 3 reintentos -> DLQ)`** y dale **Send** → responde **`202 Accepted`**. *(Espera **4 segundos**: el consumidor intenta cobrar 3 veces con espera de 1s y 2s, falla las 3 veces y lo envía a la DLQ).*
   - Abre **`pagos POST (inválido: directo a DLQ)`** y dale **Send** → responde **`202 Accepted`** (monto `-50`, va directo a la DLQ sin reintentar).
2. **Cómo comprobar que la DLQ funcionó**:
   - **En Postman**: Abre **`pagos` → `pagos fallidos (DLQ)`** (`GET http://localhost:8080/api/gimnasio/pagos/fallidos`) y dale **Send**.
     - **Qué debes ver**: Una lista JSON con los pagos en `"estado": "FALLIDO"`, mostrando `"intentos": 3` (para la tarjeta rechazada) e `"intentos": 1` (para el monto negativo), junto con el `"motivoFallo"`.
   - **En RabbitMQ Web**: En [http://localhost:15672/#/queues](http://localhost:15672/#/queues) verás la cola **`pagos.procesamiento`** (con `DLX` y `DLK` activados) y la cola **`pagos.procesamiento.dlq`**.

---

## 4. Cómo probar Kafka y ver que funcionó

En este proyecto no necesitas instalar herramientas externas para ver Kafka: el microservicio **`monitoreo-service` (`:8087`)** expone endpoints REST que te muestran en vivo lo que Kafka consumió, lo que **Kafka Streams** calculó y los **Checkpoints (offsets)** guardados.

### A. Ocupación de Clases en Tiempo Real (Topic `ocupacion-clases`)
1. **En Postman**:
   - Ve a **`clases` → `clases ingreso (Kafka)`** y dale **Send** 2 o 3 veces seguidas. Verás que cada vez responde **`200 OK`** y sube `"ocupacionActual"`.
   - También puedes darle **Send** a **`clases` → `clases salida (Kafka)`** para ver cómo baja en 1.
2. **Cómo comprobar que Kafka actualizó el Dashboard**:
   - En Postman, ve a la carpeta **`monitoreo (Kafka)` → `ocupacion actual`** (`GET http://localhost:8080/api/gimnasio/monitoreo/ocupacion`) y dale **Send**.
   - **Qué debes ver**: El JSON con todas las clases, su `"ocupacionActual"`, `"capacidadMaxima"` y el `"porcentajeOcupacion"` actualizado en tiempo real por el consumidor de Kafka.
   - *(Opcional en terminal)* Si quieres ver el stream **SSE en vivo** mientras das clic en Postman, corre en una terminal:
     ```bash
     curl -N -H "Authorization: Bearer <PEGA_AQUI_TU_ADMIN_TOKEN>" http://localhost:8087/api/gimnasio/monitoreo/ocupacion/stream
     ```

---

### B. Kafka Streams — Resumen Semanal de Entrenamientos (Ventana de 7 días)
1. **En Postman**:
   - Ve a **`miembros` → `miembros registrar entrenamiento (Kafka)`** y dale **Send** 2 veces (`200 OK`). Esto publica sesiones de `45 minutos` y `400 calorías` en el topic `datos-entrenamiento`.
2. **Cómo comprobar que Kafka Streams hizo el cálculo**:
   - Ve a **`monitoreo (Kafka)` → `resumen semanal entrenamiento (Kafka Streams)`** (`GET http://localhost:8080/api/gimnasio/monitoreo/entrenamiento/1/resumen`) y dale **Send**.
   - **Qué debes ver**: El resultado procesado por la topología de Kafka Streams (`resumen-entrenamiento-store`):
     ```json
     [
       {
         "miembroId": 1,
         "totalSesiones": 2,
         "totalMinutos": 90,
         "totalCalorias": 800,
         "sesionesPorTipo": { "CARDIO": 2 },
         "inicioVentana": "...",
         "finVentana": "..."
       }
     ]
     ```

---

### C. Recuperación ante Fallos y Checkpoints (Offsets de Kafka en BD)
1. **En Postman**:
   - Ve a **`monitoreo (Kafka)` → `estado recuperacion checkpoints (solo ADMIN)`** (`GET http://localhost:8080/api/gimnasio/monitoreo/recuperacion/estado`) y dale **Send**.
2. **Qué debes ver**:
   - La lista `"checkpoints"` mostrando cada partición de Kafka (`ocupacion-clases-0`, `datos-entrenamiento-0`, etc.) con su `"ultimoOffset"` confirmado y la cantidad total en `"eventosProcesados"`.

---

## 5. Cómo probar las Correcciones de Dominio (DDD) y Seguridad (RBAC)

En la carpeta **`seguridad (Keycloak & RBAC)`** tienes todo separado por carpetas numeradas:

1. **`02. Autenticación (401 Unauthorized)`**:
   - Petición sin token o con token falso → devuelve **`401 Unauthorized`**.
2. **`03. Autorización RBAC - Rol MEMBER (403 Forbidden)`**:
   - 8 pruebas donde un socio (`ROLE_MEMBER`) intenta hacer acciones de Admin o Entrenador → todas devuelven **`403 Forbidden`**.
3. **`04. Autorización RBAC - Rol TRAINER (403 Forbidden)`**:
   - 6 pruebas donde un entrenador (`ROLE_TRAINER`) intenta crear entrenadores, equipos, socios o ver pagos → todas devuelven **`403 Forbidden`**.
4. **`05. Pruebas de Entrada y Casos Borde` (Observaciones del Profesor)**:
   - **`3. Entrenador Inexistente devuelve 404 Not Found`** (`GET /entrenadores/999`) → devuelve **`404 Not Found`**.
   - **`4. Programar Clase con Entrenador Inexistente (404 Not Found)`** (`POST /clases` con `entrenadorId: 999`) → valida contra `entrenador-service` y devuelve **`404 Not Found`**.
   - **`5. Invariante VO Capacidad Inválida <= 0 (400 Bad Request)`** (`POST /clases` con `capacidad: -5`) → el Value Object `Capacidad` rechaza el valor con **`400 Bad Request`**.
   - **`6. Invariante VO Email Inválido (400 Bad Request)`** (`POST /miembros` con `email: "correo-sin-arroba"`) → el Value Object `Email` rechaza el valor con **`400 Bad Request`**.

---

## 6. Truco rápido: Correr las 56 pruebas solas con un clic

Si quieres ver pasar las **56 peticiones y 62 validaciones** de una sola vez en Postman:
1. Haz clic en los **tres puntos (`...`)** al lado del nombre de la colección **`gimnasio`** → **Run collection**.
2. Haz clic en el botón azul **Run gimnasio**.
3. Verás las **62 pruebas en verde (`Passed`)** en unos 4 segundos.
