# ── Etapa 1: Build ────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copia primero el pom para aprovechar cache de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copia el codigo fuente y compila
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Etapa 2: Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia solo el JAR desde la etapa de build
COPY --from=build /app/target/inventario-api-1.0.0.jar app.jar

# Puerto que expone la app (Render inyecta PORT como variable de entorno)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
