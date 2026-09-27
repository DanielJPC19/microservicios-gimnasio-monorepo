# Guía de Pruebas de Seguridad y Postman

Esta guía detalla la arquitectura de seguridad implementada en los microservicios del gimnasio, la matriz de control de acceso basada en roles (RBAC), y cómo ejecutar las pruebas automatizadas tanto en **Postman** como mediante **cURL** y **Newman (CLI)**.

---

## 1. Arquitectura de Seguridad

El sistema implementa seguridad perimetral y distribuida basada en **OAuth2 y OpenID Connect (OIDC)** con tokens **JWT (JSON Web Tokens)** gestionados por **Keycloak**:

1. **Identity and Access Management (IAM)**:
   - Servidor: Keycloak en `http://localhost:8180`
   - Realm: `gimnasio`
   - Client Público (Gateway / Postman): `gimnasio-gateway` (Direct Access Grants habilitado)
   - Clientes Confidenciales (Servicios): `gimnasio-entrenador`, `gimnasio-equipo`, `gimnasio-miembro`, `gimnasio-clase`, `gimnasio-pago`, `gimnasio-notificacion`, `gimnasio-monitoreo`.

2. **Mecanismo de Autenticación & Autorización**:
   - Cada microservicio actúa como un **OAuth2 Resource Server**.
   - Los microservicios validan la firma del JWT sin estado (*stateless*) contra los endpoints de Keycloak (`/certs`).
   - Los roles se extraen del claim `realm_access.roles` mediante `JwtAuthenticationConverter`, mapeándolos al prefijo `ROLE_`.
   - Se utiliza seguridad a nivel de método con `@PreAuthorize("hasRole(...)")` o `@PreAuthorize("hasAnyRole(...)")`.

---

## 2. Usuarios y Roles de Prueba

| Usuario | Contraseña | Rol en Realm | Propósito / Alcance |
|---------|-----------|--------------|---------------------|
| `admin` | `admin123` | `ROLE_ADMIN` | Administrador total: pagos, finanzas, alta de instructores/equipos, métricas y checkpoints. |
| `entrenador1` | `trainer123` | `ROLE_TRAINER` | Instructor: consultar y programar clases, consultar miembros, reportar averías de máquinas. |
| `miembro1` | `member123` | `ROLE_MEMBER` | Socio: consultar clases, registrar entrenamientos, registrarse y realizar pagos. |

---

## 3. Matriz de Control de Acceso (RBAC)

| Endpoint | Método | Roles Permitidos | Roles Denegados (403 Forbidden) | Sin Token (401 Unauthorized) |
|---|---|---|---|---|
| `/api/gimnasio` | GET | **Público** | Ninguno | Permite acceso (200 OK) |
| `/api/gimnasio/clases` | GET | `ADMIN`, `TRAINER`, `MEMBER` | Ninguno (autenticado) | 401 |
| `/api/gimnasio/clases` | POST | `ADMIN`, `TRAINER` | `MEMBER` | 401 |
| `/api/gimnasio/clases/{id}/horario` | PUT | `ADMIN`, `TRAINER` | `MEMBER` | 401 |
| `/api/gimnasio/clases/{id}/ingreso` | POST | `ADMIN`, `TRAINER`, `MEMBER` | Ninguno (autenticado) | 401 |
| `/api/gimnasio/clases/{id}/salida` | POST | `ADMIN`, `TRAINER`, `MEMBER` | Ninguno (autenticado) | 401 |
| `/api/gimnasio/entrenadores` | GET | `ADMIN`, `TRAINER`, `MEMBER` | Ninguno (autenticado) | 401 |
| `/api/gimnasio/entrenadores` | POST | `ADMIN` | `TRAINER`, `MEMBER` | 401 |
| `/api/gimnasio/entrenadores/{id}` | GET | `ADMIN`, `TRAINER`, `MEMBER` | Ninguno (autenticado) | 401 |
| `/api/gimnasio/equipos` | GET | `ADMIN`, `TRAINER`, `MEMBER` | Ninguno (autenticado) | 401 |
| `/api/gimnasio/equipos` | POST | `ADMIN` | `TRAINER`, `MEMBER` | 401 |
| `/api/gimnasio/equipos/{id}/reportar-averia` | POST | `ADMIN`, `TRAINER` | `MEMBER` | 401 |
| `/api/gimnasio/miembros` | GET | `ADMIN`, `TRAINER` | `MEMBER` | 401 |
| `/api/gimnasio/miembros` | POST | `ADMIN`, `MEMBER` | `TRAINER` | 401 |
| `/api/gimnasio/miembros/{id}/entrenamientos` | POST | `ADMIN`, `TRAINER`, `MEMBER` | Ninguno (autenticado) | 401 |
| `/api/gimnasio/pagos` | GET | `ADMIN` | `TRAINER`, `MEMBER` | 401 |
| `/api/gimnasio/pagos/fallidos` | GET | `ADMIN` | `TRAINER`, `MEMBER` | 401 |
| `/api/gimnasio/pagos` | POST | `ADMIN`, `MEMBER` | `TRAINER` | 401 |
| `/api/gimnasio/monitoreo/ocupacion` | GET | `ADMIN`, `TRAINER`, `MEMBER` | Ninguno (autenticado) | 401 |
| `/api/gimnasio/monitoreo/entrenamiento/{id}/resumen` | GET | `ADMIN`, `TRAINER`, `MEMBER` | Ninguno (autenticado) | 401 |
| `/api/gimnasio/monitoreo/recuperacion/estado` | GET | `ADMIN` | `TRAINER`, `MEMBER` | 401 |

---

## 4. Ejecución de Pruebas en Postman

El proyecto incluye dos archivos configurados en la raíz:
1. `gimnasio.postman_collection.json`: Colección completa con aserciones automatizadas y suite de seguridad RBAC.
2. `gimnasio.postman_environment.json`: Entorno con las URLs y variables dinámicas de tokens (`admin_token`, `trainer_token`, `member_token`, `token`).

### Paso a paso en Postman Desktop:

1. **Importar**:
   - Abrir Postman y hacer clic en **Import**.
   - Arrastrar o seleccionar los archivos `gimnasio.postman_collection.json` y `gimnasio.postman_environment.json`.
2. **Seleccionar el Entorno**:
   - En la esquina superior derecha de Postman, seleccionar el entorno **"Gimnasio Local (Docker)"**.
3. **Estructura de la Colección**:
   - Las carpetas funcionales (`clases`, `entrenadores`, `miembros`, `equipos`, `pagos`, `monitoreo (Kafka)`) prueban las funcionalidades de negocio y flujo asíncrono con aserciones automáticas de status code (`200 OK`, `202 Accepted`).
   - La carpeta **`seguridad (Keycloak & RBAC)`** contiene:
     - `01. Obtención de Tokens (AuthN)`: Logins para Admin, Trainer y Member. Guarda automáticamente cada token en su respectiva variable de entorno (`admin_token`, `trainer_token`, `member_token`). Además valida el rechazo ante credenciales erróneas.
     - `02. Autenticación (401 Unauthorized)`: Valida rechazo sin cabecera Authorization o con token manipulado.
     - `03. Autorización RBAC - Rol MEMBER (403 Forbidden)`: 8 pruebas automáticas verificando que el socio no pueda ejecutar operaciones administrativas ni de instructor.
     - `04. Autorización RBAC - Rol TRAINER (403 Forbidden)`: 6 pruebas automáticas verificando que el entrenador no pueda crear otros entrenadores, ni crear equipos, ni registrar socios, ni acceder a pagos ni checkpoints Kafka.
     - `05. Permisos Válidos por Rol (200 / 202 OK)`: Pruebas con accesos legítimos confirmando la correcta concesión de acceso.
     - `06. Pruebas de Entrada y Casos Borde`: Validación de payloads corruptos (400 Bad Request) y montos negativos a DLQ (202 Accepted).
4. **Ejecutar con Collection Runner**:
   - Hacer clic derecho sobre la carpeta **`seguridad (Keycloak & RBAC)`** o sobre toda la colección `gimnasio` -> **Run collection**.
   - Hacer clic en **Run gimnasio**.
   - Postman ejecutará secuencialmente cada petición y mostrará el informe detallado con 100% de los tests en verde (**Passed**).

---

## 5. Ejecución por Consola con Newman (CLI)

Para correr las pruebas de forma headless / automatizada en CI/CD:

```bash
# Ejecutar toda la colección
npx newman run gimnasio.postman_collection.json \
  -e gimnasio.postman_environment.json \
  --reporters cli

# Ejecutar únicamente la suite de seguridad
npx newman run gimnasio.postman_collection.json \
  -e gimnasio.postman_environment.json \
  --folder "seguridad (Keycloak & RBAC)" \
  --reporters cli
```

---

## 6. Pruebas de Seguridad Rápidas con cURL

### 1. Obtener los 3 tokens:
```bash
# Admin
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8180/realms/gimnasio/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=gimnasio-gateway" \
  -d "username=admin" -d "password=admin123" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Entrenador
TRAINER_TOKEN=$(curl -s -X POST http://localhost:8180/realms/gimnasio/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=gimnasio-gateway" \
  -d "username=entrenador1" -d "password=trainer123" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Miembro
MEMBER_TOKEN=$(curl -s -X POST http://localhost:8180/realms/gimnasio/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=gimnasio-gateway" \
  -d "username=miembro1" -d "password=member123" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### 2. Prueba 401 Unauthorized (Sin token y token falso):
```bash
# Petición sin token -> 401 Unauthorized
curl -i http://localhost:8080/api/gimnasio/clases

# Petición con token corrupto -> 401 Unauthorized
curl -i -H "Authorization: Bearer token_falso_invalido" http://localhost:8080/api/gimnasio/clases
```

### 3. Prueba 403 Forbidden (RBAC - Restricciones por rol):
```bash
# Miembro intentando listar todos los miembros (solo ADMIN y TRAINER) -> 403 Forbidden
curl -i -H "Authorization: Bearer $MEMBER_TOKEN" http://localhost:8080/api/gimnasio/miembros

# Miembro intentando programar clase (solo ADMIN y TRAINER) -> 403 Forbidden
curl -i -X POST http://localhost:8080/api/gimnasio/clases \
  -H "Authorization: Bearer $MEMBER_TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre": "Yoga Ilegal", "horario": "2026-09-01T10:00:00", "capacidad": 20, "entrenadorId": 1}'

# Entrenador intentando crear un nuevo entrenador (solo ADMIN) -> 403 Forbidden
curl -i -X POST http://localhost:8080/api/gimnasio/entrenadores \
  -H "Authorization: Bearer $TRAINER_TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre": "Nuevo Entrenador", "especialidad": "Boxeo"}'

# Entrenador intentando ver los pagos (solo ADMIN) -> 403 Forbidden
curl -i -H "Authorization: Bearer $TRAINER_TOKEN" http://localhost:8080/api/gimnasio/pagos
```

### 4. Prueba 200 OK / 202 Accepted (Permisos autorizados):
```bash
# Entrenador listando miembros -> 200 OK
curl -i -H "Authorization: Bearer $TRAINER_TOKEN" http://localhost:8080/api/gimnasio/miembros

# Miembro consultando clases disponibles -> 200 OK
curl -i -H "Authorization: Bearer $MEMBER_TOKEN" http://localhost:8080/api/gimnasio/clases

# Admin consultando pagos -> 200 OK
curl -i -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/gimnasio/pagos

# Miembro realizando pago -> 202 Accepted
curl -i -X POST http://localhost:8080/api/gimnasio/pagos \
  -H "Authorization: Bearer $MEMBER_TOKEN" -H "Content-Type: application/json" \
  -d '{"miembroId": 1, "monto": 120000, "metodoPago": "TARJETA"}'
```
