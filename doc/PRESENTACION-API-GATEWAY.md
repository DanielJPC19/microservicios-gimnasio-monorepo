# Explicación Técnica de la Implementación: API Gateway para Microservicios de Gimnasio

> **Equipo**  
> - Daniel Jose Plazas Cortes (A00400085)  
> - Rony Farid Ordoñez García (A00397968)  
> - Daniel Gonzales Rivera (A00399873)  
> - Juan Pablo Parra Betancourt (A00)  

---

## 1. Introducción y Propósito del Componente

En la arquitectura de microservicios desarrollada para el sistema del gimnasio, cada servicio de dominio (`miembro-service`, `clase-service`, `entrenador-service`, `equipo-service`, `pago-service`, `monitoreo-service`) opera de forma autónoma con su propio ciclo de vida y almacenamiento de datos.

El componente `api-gateway` se implementó como el punto único de entrada perimetral al sistema. Su función consiste en recibir todas las solicitudes externas en el puerto `8080`, aplicar autenticación y autorización centralizada, resolver dinámicamente las instancias de destino mediante descubrimiento de servicios y balanceo de carga, y proporcionar un punto de agregación de datos para consultas compuestas.

La solución se construyó utilizando Spring Boot `3.3.2` y Spring Cloud `2023.0.3` sobre el modelo reactivo de Spring Cloud Gateway (Netty y Spring WebFlux), evitando el bloqueo de hilos ante múltiples peticiones concurrentes.

---

## 2. Estructura del Proyecto y Dependencias

El código fuente del Gateway se ubica en el directorio `services/api-gateway`. Las dependencias definidas en `pom.xml` corresponden a los requisitos funcionales del componente:

- `spring-cloud-starter-gateway`: Proveedor del motor de enrutamiento reactivo basado en Netty, filtros globales y predicados de ruta.
- `spring-cloud-starter-netflix-eureka-client`: Cliente de descubrimiento para registrar el Gateway ante Eureka Server y consultar las instancias activas de los microservicios.
- `spring-cloud-starter-loadbalancer`: Proveedor del balanceo de carga reactivo en el cliente para distribuir solicitudes entre instancias registradas.
- `spring-boot-starter-security` y `spring-boot-starter-oauth2-resource-server`: Soporte para seguridad reactiva (`ServerHttpSecurity`) y validación de tokens JWT emitidos por Keycloak.
- `spring-boot-starter-actuator`: Exposición de endpoints de auditoría y diagnóstico del Gateway, como `/actuator/health` y `/actuator/gateway/routes`.

---

## 3. Configuración del Enrutamiento

El enrutamiento de peticiones se estructuró combinando dos mecanismos: configuración declarativa en archivo YAML y configuración programática mediante código Java. Ambas definiciones se consolidan en tiempo de ejecución en la tabla de rutas interna de Spring Cloud Gateway.

### 3.1 Enrutamiento Declarativo (`application.yml`)
En `services/api-gateway/src/main/resources/application.yml` se configuraron las rutas para los microservicios principales del gimnasio:

- `ruta-miembros`: Atiende el patrón `/api/gimnasio/miembros/**` y reenvía hacia `lb://miembro-service`.
- `ruta-clases`: Atiende el patrón `/api/gimnasio/clases/**` y reenvía hacia `lb://clase-service`.
- `ruta-entrenadores`: Atiende el patrón `/api/gimnasio/entrenadores/**` y reenvía hacia `lb://entrenador-service`.
- `ruta-pagos`: Atiende el patrón `/api/gimnasio/pagos/**` y reenvía hacia `lb://pago-service`.
- `ruta-monitoreo`: Atiende el patrón `/api/gimnasio/monitoreo/**` y reenvía hacia `lb://monitoreo-service`.

Se estableció la propiedad `spring.cloud.gateway.discovery.locator.enabled: false` para asegurar que únicamente las rutas explícitamente declaradas sean accesibles, evitando la exposición no controlada de servicios internos de Eureka.

### 3.2 Enrutamiento Programático (`GatewayRoutesConfig.java`)
En la clase `co.analisys.gimnasio.config.GatewayRoutesConfig` se implementó la ruta correspondiente al módulo de equipos mediante un bean `RouteLocator`:

- Utiliza `RouteLocatorBuilder` para definir la ruta `ruta-equipos-programatica`.
- Asocia el predicado de ruta `/api/gimnasio/equipos/**` al destino `lb://equipo-service`.

Ambos esquemas utilizan el prefijo `lb://` en lugar de URLs fijas, delegando la resolución de red al balanceador de carga. El microservicio de notificaciones (`notificacion-service`) no cuenta con ruta configurada en el Gateway debido a que opera exclusivamente como consumidor de eventos asíncronos en RabbitMQ.

---

## 4. Implementación de Seguridad con Keycloak

La seguridad del sistema está organizada mediante el estándar OAuth2 y OpenID Connect, utilizando Keycloak como servidor de identidad y autorización, y configurando el API Gateway como un Resource Server reactivo.

### 4.1 Configuración del Realm (`gimnasio-realm.json`)
El archivo `keycloak/realm/gimnasio-realm.json` define la configuración que se importa automáticamente en el contenedor de Keycloak (`:8180`):
- **Realm:** `gimnasio`.
- **Roles:** `ROLE_ADMIN`, `ROLE_TRAINER` y `ROLE_MEMBER`.
- **Cliente público:** `gimnasio-gateway`, configurado con soporte para obtención de tokens vía usuario y contraseña (`directAccessGrantsEnabled: true`).
- **Usuarios configurados para validación:**
  - `admin` (credencial: `admin123`, rol: `ROLE_ADMIN`).
  - `entrenador1` (credencial: `trainer123`, rol: `ROLE_TRAINER`).
  - `miembro1` (credencial: `member123`, rol: `ROLE_MEMBER`).

### 4.2 Cadena de Seguridad en el Gateway (`SecurityConfig.java`)
En `co.analisys.gimnasio.config.SecurityConfig` se configuró la cadena de filtros reactiva (`SecurityWebFilterChain`) utilizando `ServerHttpSecurity`:

1. **Protección CSRF:** Se deshabilita debido a que la API es de tipo stateless y las peticiones se autentican mediante tokens en la cabecera `Authorization`.
2. **Control de acceso por roles y métodos HTTP:**
   - Acceso público: `/actuator/**` y `/api/gimnasio`.
   - Restricción estricta de administración: `POST` en `/api/gimnasio/entrenadores/**` y `GET` en `/api/gimnasio/pagos` requieren rol `ADMIN`.
   - Operaciones de entrenadores y administración: `POST` en `/api/gimnasio/equipos/**`, `PUT` en `/api/gimnasio/clases/**` y `GET` en `/api/gimnasio/monitoreo/**` requieren rol `ADMIN` o `TRAINER`.
   - Operaciones de miembros: `POST` en `/api/gimnasio/miembros` permite `ADMIN` o `MEMBER`; la consulta individual `GET /api/gimnasio/miembros/*` permite `ADMIN`, `TRAINER` o `MEMBER`; la consulta general `GET /api/gimnasio/miembros` se restringe a `ADMIN` o `TRAINER`.
   - Endpoint de agregación: `/api/gimnasio/agregacion/**` es accesible por cualquiera de los tres roles autenticados.
   - Cualquier otra ruta no explícita exige autenticación obligatoria (`anyExchange().authenticated()`).

### 4.3 Extracción y Conversión de Roles
Keycloak ubica los roles del usuario dentro del claim `realm_access.roles` del token JWT. La clase `SecurityConfig` incluye el método `reactiveJwtAuthenticationConverter()`, el cual extrae dicha lista del claim, antepone el prefijo `ROLE_` y genera las instancias de `SimpleGrantedAuthority` requeridas por Spring Security para evaluar expresiones como `hasRole(...)`.

### 4.4 Forwarding de Tokens JWT
Para mantener un esquema de defensa en profundidad, los microservicios downstream también validan la identidad de la petición:
- En las rutas gestionadas por Spring Cloud Gateway, el filtro interno de Netty conserva y propaga automáticamente la cabecera `Authorization: Bearer <token>` hacia los microservicios de destino.
- En las peticiones manuales realizadas desde el Gateway (agregación), el controlador lee la cabecera entrante mediante `@RequestHeader` y la replica explícitamente en cada llamada realizada con `WebClient`.

---

## 5. Balanceo de Carga y Descubrimiento con Eureka

El sistema desacopla la ubicación física de los microservicios mediante registro dinámico y balanceo en el cliente.

### 5.1 Registro en Eureka Server
- El servicio de descubrimiento se implementó en `services/eureka-server` (puerto `8761`) utilizando la anotación `@EnableEurekaServer`.
- Cada microservicio y el API Gateway incluyen `spring-cloud-starter-netflix-eureka-client` y publican su estado periódicamente en `http://eureka-server:8761/eureka/`.
- En la clase principal del Gateway (`GimnasioApplication.java`) se incluyó la anotación `@EnableDiscoveryClient`.

### 5.2 Resolución mediante Nombres de Servicio (`lb://`)
Al definir una URI con la sintaxis `lb://<nombre-servicio>` (por ejemplo, `lb://clase-service`), Spring Cloud Gateway activa el filtro `ReactiveLoadBalancerClientFilter`. Este filtro consulta la lista de instancias saludables disponibles en el catálogo de Eureka y utiliza Spring Cloud LoadBalancer para seleccionar una de ellas mediante el algoritmo Round-Robin.

### 5.3 Demostración con Múltiples Instancias
En el archivo `docker-compose.yml` se configuraron dos instancias del microservicio de clases compartiendo el mismo nombre lógico de aplicación (`clase-service`):
- `clase-service`: instancia en puerto host `8084` (puerto interno `8084`).
- `clase-service-2`: instancia en puerto host `8088` (puerto interno `8084`).

Al realizar peticiones consecutivas al Gateway en la ruta `/api/gimnasio/clases`, el balanceador distribuye las solicitudes alternadamente entre ambas instancias, lo cual es verificable tanto en el panel de Eureka (`http://localhost:8761`) como en los registros de ejecución de los contenedores.

---

## 6. Filtro de Agregación de Respuestas (API Composition)

Para dar respuesta al requerimiento de agregación de información proveniente de múltiples servicios, se implementó el patrón de composición de API en el Gateway.

### 6.1 Justificación del Diseño
La agregación se implementó mediante un controlador reactivo (`ResumenMiembroAggregationController`) apoyado en un `WebClient` con soporte para balanceo de carga (`@LoadBalanced`), en lugar de utilizar un filtro de manipulación de bajo nivel de cuerpo de respuesta (`ModifyResponseBodyGatewayFilterFactory`). Esta decisión técnica obedece a tres motivos:
1. Permite ejecutar llamadas concurrentes no bloqueantes hacia varios microservicios en paralelo.
2. Evita almacenar en búfer streams completos de respuesta HTTP en memoria intermedia.
3. Facilita el manejo de excepciones y degradación elegante de manera independiente por cada servicio consultado.

### 6.2 Componentes de la Implementación

**1. Configuración de `WebClient` (`WebClientConfig.java`):**
En `co.analisys.gimnasio.config.WebClientConfig` se expone un bean `WebClient.Builder` anotado con `@LoadBalanced`, lo que permite que las URIs utilicen el prefijo `lb://` y resuelvan las instancias a través de Eureka.

**2. Controlador de Agregación (`ResumenMiembroAggregationController.java`):**
Ubicado en `co.analisys.gimnasio.controller`, expone el endpoint:
`GET /api/gimnasio/agregacion/miembro/{id}`

El flujo de ejecución del endpoint se compone de los siguientes pasos:
1. **Llamada a `miembro-service`:** Consulta `lb://miembro-service/api/gimnasio/miembros/{id}` para obtener los datos básicos del socio. Si el servicio no responde, captura el fallo con `onErrorResume` y retorna un objeto de miembro predeterminado.
2. **Llamada a `clase-service`:** Consulta `lb://clase-service/api/gimnasio/clases` para obtener la oferta de clases activas del gimnasio. Si falla, retorna una lista vacía.
3. **Llamada a `pago-service`:** Consulta `lb://pago-service/api/gimnasio/pagos`. Transforma reactivamente la lista para filtrar únicamente aquellos pagos cuyo `miembroId` coincide con el identificador consultado. En caso de error o restricción de permisos, retorna una lista vacía.
4. **Sincronización concurrente:** Utiliza el operador reactivo `Mono.zip(miembroMono, clasesMono, pagosMono)`, disparando las tres peticiones simultáneamente y esperando a que todas completen para ensamblar el resultado final.
5. **Propagación del token:** La cabecera `Authorization` recibida se transfiere a cada una de las tres llamadas salientes.

### 6.3 Modelo de Datos Consolidado (`ResumenMiembroDTO.java`)
La respuesta consolidada se estructura mediante la clase `co.analisys.gimnasio.dto.ResumenMiembroDTO`, la cual contiene:
- `miembro`: Objeto con identificación, nombre, correo y fecha de inscripción del socio (`MiembroDTO`).
- `clasesDisponibles`: Lista de clases programadas en el gimnasio con sus detalles de horario, capacidad y entrenador.
- `pagosRegistrados`: Historial de transacciones de pago asociadas al miembro consultado.
- `mensaje`: Cadena descriptiva indicando el origen consolidado de la información.

---

## 7. Verificación del Funcionamiento

Para confirmar la operación del API Gateway en el entorno local con Docker Compose:

1. **Estado general de los servicios:**
   Al ejecutar `docker compose up -d`, los servicios `eureka-server`, `keycloak`, `api-gateway` y los microservicios de dominio inician en la red compartida.
2. **Verificación de rutas en Actuator:**
   Al consultar `http://localhost:8080/actuator/gateway/routes`, el Gateway responde con el listado JSON de las rutas activas, incluyendo tanto las definidas en `application.yml` como la ruta programática de equipos.
3. **Verificación en Eureka:**
   En `http://localhost:8761`, la interfaz de Eureka muestra registrados todos los microservicios, reflejando 2 instancias para `CLASE-SERVICE`.
4. **Pruebas de seguridad perimetral:**
   - Peticiones sin cabecera `Authorization` retornan código HTTP `401 Unauthorized`.
   - Peticiones con token de usuario `MEMBER` hacia rutas administrativas (como `GET /api/gimnasio/pagos`) retornan código HTTP `403 Forbidden`.
   - Peticiones con token de usuario `ADMIN` retornan código HTTP `200 OK`.
5. **Prueba de agregación:**
   Al invocar `GET /api/gimnasio/agregacion/miembro/1` con un token válido, el Gateway entrega la estructura consolidada en una única respuesta HTTP, manteniendo la resiliencia en caso de detención de algún servicio secundario.

---

## 8. Resumen de Archivos Implementados

A continuación se listan los archivos principales en el repositorio que sustentan la solución para facilitar su revisión directa en el código:

| Archivo | Ubicación en el Repositorio | Función Principal |
|---|---|---|
| `pom.xml` | `services/api-gateway/pom.xml` | Declaración de dependencias reactivas, seguridad, Eureka y LoadBalancer |
| `application.yml` | `services/api-gateway/src/main/resources/application.yml` | Configuración de rutas declarativas (`lb://`), Keycloak JWKS, Eureka y Actuator |
| `GatewayRoutesConfig.java` | `services/api-gateway/src/main/java/co/analisys/gimnasio/config/GatewayRoutesConfig.java` | Configuración de enrutamiento programático (`RouteLocator`) |
| `SecurityConfig.java` | `services/api-gateway/src/main/java/co/analisys/gimnasio/config/SecurityConfig.java` | Cadena reactiva de seguridad, reglas RBAC y conversor de roles de Keycloak |
| `WebClientConfig.java` | `services/api-gateway/src/main/java/co/analisys/gimnasio/config/WebClientConfig.java` | Definición de `WebClient.Builder` con `@LoadBalanced` |
| `ResumenMiembroAggregationController.java` | `services/api-gateway/src/main/java/co/analisys/gimnasio/controller/ResumenMiembroAggregationController.java` | Controlador reactivo de agregación con `Mono.zip` y tolerancia a fallos |
| `ResumenMiembroDTO.java` | `services/api-gateway/src/main/java/co/analisys/gimnasio/dto/ResumenMiembroDTO.java` | DTO con la estructura consolidada de respuesta |
| `gimnasio-realm.json` | `keycloak/realm/gimnasio-realm.json` | Configuración del realm de Keycloak, roles, clientes y usuarios |
| `docker-compose.yml` | `docker-compose.yml` | Orquestación de contenedores, variables de entorno y réplicas de `clase-service` |
