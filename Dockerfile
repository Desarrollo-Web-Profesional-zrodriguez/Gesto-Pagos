# Etapa 1: Construcción con Java 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copiar archivos de configuración de Gradle
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Eliminar posibles saltos de línea Windows (CRLF) y dar permisos de ejecución
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# Copiar el código fuente del proyecto
COPY src ./src

# Compilar el JAR excluyendo los tests
RUN ./gradlew bootJar -x test --no-daemon

# Etapa 2: Imagen de ejecución ligera
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el archivo JAR generado desde la etapa de construcción
COPY --from=builder /app/build/libs/*.jar app.jar

# Configurar el puerto dinámico para Railway
ENV PORT=8080
EXPOSE 8080

# Iniciar la aplicación respetando el puerto inyectado por Railway
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
