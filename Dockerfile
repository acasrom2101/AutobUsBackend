# Etapa 1: Construcción (Descarga Maven y compila el código)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Crea un entorno ligero solo con Java para correr la app)
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copia el .jar que se generó en la etapa 1
COPY --from=build /app/target/*.jar app.jar
# Expone el puerto 8080 (el que usa Spring Boot por defecto)
EXPOSE 8080
# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]