FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY cli/pom.xml cli/pom.xml
COPY api/pom.xml api/pom.xml
RUN mvn -B -pl api -am dependency:go-offline

COPY core/src core/src
COPY api/src api/src
RUN mvn -B -pl api -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S archguard && adduser -S archguard -G archguard \
    && apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/api/target/archguard-api-0.1.0-SNAPSHOT.jar app.jar
RUN chown archguard:archguard app.jar
USER archguard
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
