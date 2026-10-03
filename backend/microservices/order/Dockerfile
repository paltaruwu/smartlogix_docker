FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy all POM files for Maven reactor discovery
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

# Cache dependencies
RUN mvn -pl microservices/order -am dependency:go-offline -DskipTests --no-transfer-progress 2>/dev/null || true

# Copy source for this module
COPY microservices/order/src ./microservices/order/src

# Build
RUN mvn -pl microservices/order -am package -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/microservices/order/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
