FROM maven:3-amazoncorretto-25-alpine AS build
WORKDIR /app

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q package -DskipTests

FROM amazoncorretto:25-alpine
WORKDIR /app

COPY --from=build /app/target/oficinamecanicaapi-*.jar app.jar

USER nobody
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
