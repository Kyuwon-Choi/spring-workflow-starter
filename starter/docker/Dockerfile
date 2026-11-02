# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw npmw scripts/build.sh && ./scripts/build.sh

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN groupadd --gid 10001 solution \
    && useradd --uid 10001 --gid solution --create-home solution \
    && mkdir -p /app/.local-data \
    && chown -R solution:solution /app
WORKDIR /app
COPY --from=build --chown=solution:solution /workspace/target/solution-0.0.1-SNAPSHOT.jar /app/solution.jar
USER 10001:10001
ENV SERVER_ADDRESS=0.0.0.0 SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/solution.jar"]
