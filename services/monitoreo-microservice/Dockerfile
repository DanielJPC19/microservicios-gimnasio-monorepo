FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
# Kafka Streams usa RocksDB (librería nativa) para el state store y necesita libstdc++, ausente en alpine.
RUN apk add --no-cache libstdc++
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
