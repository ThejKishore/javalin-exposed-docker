# Multi-stage build with jlink custom runtime

# 1) Build stage: use JDK 21 to build the app
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Leverage Gradle wrapper
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY src src

# Build the application (production)
RUN ./gradlew --no-daemon clean build



FROM eclipse-temurin:21-jre AS runtime
# Copy the fat jar built by shadowJar from the build stage
COPY --from=build /app/build/libs/javalin-example-*-all.jar /app.jar
# This is the port that your javalin application will listen on
EXPOSE 7070
ENTRYPOINT ["java", "-jar", "/app.jar"]