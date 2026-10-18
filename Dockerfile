FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B -DskipTests package
FROM eclipse-temurin:25-jre
# O pebble da imagem base não é usado pela aplicação e traz CVEs barradas pelo Trivy.
RUN rm -f /usr/bin/pebble
WORKDIR /app
COPY --from=build /app/target/loja-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
