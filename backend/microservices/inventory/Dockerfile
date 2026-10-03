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

RUN mvn -pl microservices/inventory -am dependency:go-offline -DskipTests --no-transfer-progress 2>/dev/null || true

COPY microservices/inventory/src ./microservices/inventory/src

RUN mvn -pl microservices/inventory -am package -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/microservices/inventory/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
