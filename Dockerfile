# =============================================
# Build stage
# =============================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Descarga dependencias primero (layer caching)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# =============================================
# Run stage
# =============================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Crear directorio para archivos de info (compatibilidad)
RUN mkdir -p /app/info

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Activa el perfil de producción (MCP via SSE, sin Docker CLI)
ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
