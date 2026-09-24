FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

RUN useradd --create-home builder && chown builder:builder /build
USER builder
COPY --chown=builder:builder pom.xml .
RUN mvn -B -Dmaven.repo.local=/home/builder/.m2/repository dependency:go-offline

COPY --chown=builder:builder src ./src
RUN mvn -B -Dmaven.repo.local=/home/builder/.m2/repository package

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
