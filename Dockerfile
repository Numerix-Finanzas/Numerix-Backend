# the base image
FROM eclipse-temurin:17-jre
# Zona horaria de Lima para las fechas de compras y pagos
ENV TZ=America/Lima
# the JAR file path (generar antes con: ./mvnw clean package -DskipTests)
ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} backend.jar

# Exponer el puerto HTTP
EXPOSE 8080

ENTRYPOINT ["java", "-Xmx1024M", "-jar", "/backend.jar"]
