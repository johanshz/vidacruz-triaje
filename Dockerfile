# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN sed -i 's/\r$//' scripts/descargar-modelo.sh \
    && chmod +x scripts/descargar-modelo.sh \
    && ./scripts/descargar-modelo.sh infraestructura/src/main/resources/model
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon :infraestructura:bootJar

FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system asistente \
    && useradd --system --gid asistente --home-dir /app asistente
WORKDIR /app
COPY --from=build /workspace/infraestructura/build/libs/infraestructura-0.0.1-SNAPSHOT.jar /app/asistente.jar
USER asistente
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/asistente.jar"]
