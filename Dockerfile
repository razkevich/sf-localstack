# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY service/pom.xml service/
COPY client/pom.xml client/
RUN mvn dependency:go-offline -pl service -q
COPY service/src service/src
RUN mvn -pl service package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/service/target/sf-localstack-service-*.jar sf-localstack.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/sf-localstack.jar"]
