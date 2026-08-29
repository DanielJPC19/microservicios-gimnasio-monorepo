# Gimnasio - Arquitectura de Microservicios

Refactorización de un monolito Spring Boot de gestión de gimnasio en 4 microservicios independientes aplicando Domain-Driven Design (DDD). Taller académico — ver [`doc/project.md`](doc/project.md) para el enunciado completo.

## Equipo

1. Daniel Jose Plazas Cortes - A00400085
2. Rony Farid Ordoñez García - A00397968
3. Daniel Gonzales Rivera - A00
4. Juan Pablo Parra Betancourt - A00

## Arquitectura

| Servicio | Puerto | Dominio |
|---|---|---|
| `api-gateway` | 8080 | Api Gateway |
| `clase-microservice` | 8084 | Programación de clases |
| `entrenador-microservice` | 8081 | Gestión de entrenadores |
| `equipo-microservice` | 8082 | Inventario de equipos |
| `miembro-microservice` | 8083 | Gestión de miembros |

Comunicación síncrona REST punto-a-punto: `clase-service` consulta a `entrenador-service` para enriquecer la respuesta de clases con los datos del entrenador asignado. El resto son standalone. Cada servicio persiste en su propia instancia H2 en memoria.

Detalle completo (modelos, endpoints, decisiones y limitaciones conocidas): [`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md). Diagrama de despliegue: [`doc/DeploymentDiagram.drawio`](doc/DeploymentDiagram.drawio).

## Estructura del repo

```
services/
  api-gateway/
  clase-microservice/
  entrenador-microservice/
  equipo-microservice/
  miembro-microservice/
doc/
pom.xml            # aggregator, compila los 4 módulos
docker-compose.yml
```

## Cómo correr

**Con Docker Compose (recomendado para demo):**
```bash
docker-compose up --build
```

**Sin Docker, cada servicio suelto:**
```bash
./mvnw compile              # compila los 4 desde la raíz (aggregator)
cd services/entrenador-microservice && ./mvnw spring-boot:run   # arrancar primero
cd services/clase-microservice && ./mvnw spring-boot:run        # depende de entrenador
cd services/equipo-microservice && ./mvnw spring-boot:run
cd services/miembro-microservice && ./mvnw spring-boot:run
cd services/api-gateway && ./mvnw spring-boot:run               # depende de todos los microservicios
```

## Probar

Para cada microservicio de manera independiente, se pueden realizar las siguientes request:

```bash
curl http://localhost:8083/api/gimnasio/miembros
curl http://localhost:8081/api/gimnasio/entrenadores
curl http://localhost:8082/api/gimnasio/equipos
curl http://localhost:8084/api/gimnasio/clases      # incluye datos del entrenador (llamada cross-service)
```

Para hacer la petición hacia el api gateway, realizar las mismas peticiones pero en el puerto 8080:

```bash
curl http://localhost:8080/api/gimnasio/miembros
curl http://localhost:8080/api/gimnasio/entrenadores
curl http://localhost:8080/api/gimnasio/equipos
curl http://localhost:8080/api/gimnasio/clases
```

También puede abrir en Postman la colección de las request para cada endpoint en el archivo `gimnasio.postman_collection.json`