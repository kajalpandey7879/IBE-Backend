FROM eclipse-temurin:17-jre-alpine

# Install curl — needed for ECS container health check (CMD-SHELL)
RUN apk add --no-cache curl

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]