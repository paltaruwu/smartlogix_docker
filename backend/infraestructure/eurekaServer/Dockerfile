# Contexto de build: ./backend (el pom hereda de infraestructure/pom.xml)
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

COPY pom.xml ./
COPY microservices/pom.xml ./microservices/
COPY microservices/order/pom.xml ./microservices/order/
COPY microservices/inventory/pom.xml ./microservices/inventory/
COPY microservices/shipment/pom.xml ./microservices/shipment/
COPY microservices/bff/pom.xml ./microservices/bff/
COPY microservices/rabbitadmin/pom.xml ./microservices/rabbitadmin/
COPY infraestructure/pom.xml ./infraestructure/
COPY infraestructure/apiGateway/pom.xml ./infraestructure/apiGateway/
COPY infraestructure/eurekaServer/pom.xml ./infraestructure/eurekaServer/
COPY infraestructure/keyCloakAdapter/pom.xml ./infraestructure/keyCloakAdapter/
COPY infraestructure/springBootAdmin1/pom.xml ./infraestructure/springBootAdmin1/

RUN mvn -pl infraestructure/eurekaServer -am dependency:go-offline -DskipTests --no-transfer-progress 2>/dev/null || true

COPY infraestructure/eurekaServer/src ./infraestructure/eurekaServer/src

RUN mvn -pl infraestructure/eurekaServer -am package -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/infraestructure/eurekaServer/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
