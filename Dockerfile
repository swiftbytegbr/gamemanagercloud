# syntax=docker/dockerfile:1.6

# deps
FROM maven:3.9-eclipse-temurin-25 AS deps
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests dependency:go-offline

# build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -T 1C -DskipTests -Dreproducible=true package

# runtime
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /workspace/target/*.jar /app/app.jar
ARG SPRING_PROFILE=local
ENV SPRING_PROFILES_ACTIVE=$SPRING_PROFILE
ENV JAVA_OPTS="-XX:InitialRAMPercentage=25 -XX:MaxRAMPercentage=75"
USER spring
EXPOSE 8080
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
