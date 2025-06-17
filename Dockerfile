# ---------- Step 1: Build using Maven Wrapper ----------
FROM eclipse-temurin:18-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# ---------- Step 2: Run the Spring Boot JAR ----------
FROM eclipse-temurin:18-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]
