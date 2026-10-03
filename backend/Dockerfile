# Dockerfile genérico para TODOS los servicios del backend (reactor Maven).
# Se reutiliza para eurekaServer, apiGateway, inventory, order, shipment, bff, etc.
# El módulo a compilar se indica con --build-arg MODULE=... en docker-compose.yml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ARG MODULE
COPY ${MODULE}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

