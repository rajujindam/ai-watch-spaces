# Use Java 21 and Maven to build the application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

# Use a smaller Java runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/ai-watch-spaces-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
