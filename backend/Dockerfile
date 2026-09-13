# Dockerfile genérico para TODOS los servicios del backend (reactor Maven).
# Se reutiliza para eurekaServer, apiGateway, inventory, order, shipment, bff, etc.
# El módulo a compilar se indica con --build-arg MODULE=... en docker-compose.yml

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos todo el código del backend (necesario porque es un reactor multi-módulo:
# Maven necesita ver todos los pom.xml para resolver el árbol de módulos)
COPY . .

ARG MODULE
RUN mvn -q -pl ${MODULE} -am -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ARG MODULE
COPY --from=build /app/${MODULE}/target/*.jar app.jar

# Variables típicas que se sobreescriben desde docker-compose (Spring Boot
# relaxed binding: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE, SPRING_DATASOURCE_URL, etc.)
ENTRYPOINT ["java", "-jar", "app.jar"]
