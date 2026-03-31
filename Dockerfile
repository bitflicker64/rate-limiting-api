FROM maven:3.9-amazoncorretto-21 AS backend
WORKDIR /backend
COPY pom.xml .
COPY lombok.config .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean install -DskipITs
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM eclipse-temurin:21-jre
LABEL maintainer="bitflicker64"
LABEL description="Rate Limiting API with JWT and Redis"
ARG DEPENDENCY=/backend/target/dependency
COPY --from=backend ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=backend ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=backend ${DEPENDENCY}/BOOT-INF/classes /app
EXPOSE 8080
ENTRYPOINT ["java","-cp","app:app/lib/*","com.behl.overseer.RateLimitingApiApplication"]
