FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests -B && \
    java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:25-jre-alpine
LABEL org.opencontainers.image.source="https://github.com/rodrigouma04/fennixs"
LABEL org.opencontainers.image.description="Fennixs auth service"
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring && \
    apk add --no-cache wget
COPY --from=builder --chown=spring:spring /app/target/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/target/extracted/application/ ./
USER spring
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
