# Multi-stage build with jlink custom runtime

# 1) Build stage: use JDK 21 to build the app
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Leverage Gradle wrapper
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
# Copy multi-module sources and build scripts
COPY app app
COPY shared shared
COPY user user
COPY gradle.properties gradle.properties

# Build the application (production)
RUN chmod +x gradlew && ./gradlew --no-daemon -I gradle/local-init.gradle clean build



FROM eclipse-temurin:21-jre AS runtime
# Copy the fat jar built by shadowJar from the build stage (app module)
COPY --from=build /app/app/build/libs/app-*-all.jar /app.jar
# This is the port that your javalin application will listen on
EXPOSE 7070
ENTRYPOINT ["java", "-jar", "/app.jar"]