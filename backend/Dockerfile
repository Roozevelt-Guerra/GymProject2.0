FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --no-create-home app \
    && mkdir -p /app/data \
    && chown app:app /app/data

COPY --from=build /build/target/maqpro-api-*.jar /app/app.jar

ENV CATALOG_FILE=/app/data/equipment.json
USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
