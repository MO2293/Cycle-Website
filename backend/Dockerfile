# ---- Stage 1: build ----
# The build needs Maven and a full JDK; the finished application does not.
# Doing this in a separate stage keeps the compiler, the source and the whole
# ~/.m2 cache out of the shipped image.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the POM alone first. Docker caches each layer, so dependency download is
# only re-run when the POM actually changes — editing a Java file no longer
# means re-downloading every dependency.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Stage 2: runtime ----
# JRE, not JDK: nothing here compiles anything.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a non-root user. If the application is ever compromised, the attacker
# lands as an unprivileged account rather than as root inside the container.
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /build/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring
EXPOSE 8080

# Container-aware memory settings: without this the JVM sizes its heap from the
# host's total RAM and ignores the container limit, which is a classic way to
# get killed by the OOM killer in production.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
