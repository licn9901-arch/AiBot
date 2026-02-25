FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/mqtt-gateway-*.jar app.jar
COPY src/main/resources/config.yaml config.yaml
EXPOSE 1883 8081
ENTRYPOINT ["java", "-Dgateway.config=/app/config.yaml", "-jar", "app.jar"]
