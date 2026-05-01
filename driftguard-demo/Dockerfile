FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
COPY driftguard-core/pom.xml driftguard-core/pom.xml
COPY driftguard-algorithms/pom.xml driftguard-algorithms/pom.xml
COPY driftguard-testkit/pom.xml driftguard-testkit/pom.xml
COPY driftguard-kafka/pom.xml driftguard-kafka/pom.xml
COPY driftguard-spring-boot-starter/pom.xml driftguard-spring-boot-starter/pom.xml
COPY driftguard-demo/pom.xml driftguard-demo/pom.xml

RUN mvn -pl driftguard-demo -am dependency:go-offline

COPY . .

RUN mvn -pl driftguard-demo -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/driftguard-demo/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]