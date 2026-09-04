FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Cache-buster: this RUN consumes the ARG, so changing CACHEBUST invalidates ALL
# subsequent layers (ENV, COPY, ENTRYPOINT). Required because SnapDeploy "retry"
# reuses Docker build cache and won't rebuild otherwise.
ARG CACHEBUST=metaspace-fix-v2
RUN echo "cachebust:${CACHEBUST}" > /tmp/cachebust \
    && rm -f /tmp/cachebust \
    && groupadd --system app && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/uploads/profile-pictures /app/backup \
    && chown -R app:app /app
COPY --from=build /app/target/*.jar /app/app.jar
# Seed existing profile photos from the local lab (DB paths like /uploads/profiles/3.jpg)
COPY seed-uploads/ /app/uploads/profile-pictures/
RUN chown -R app:app /app/uploads
ENV APP_BROWSER_OPEN_ON_START=false \
    SPRING_PROFILES_ACTIVE=cloud
USER app
EXPOSE 8080
# JVM flags passed directly on command line to override any cached JAVA_TOOL_OPTIONS env var
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=50.0", "-XX:+UseSerialGC", "-Xss256k", "-XX:MaxMetaspaceSize=256m", "-XX:CompressedClassSpaceSize=96m", "-XX:+ExitOnOutOfMemoryError", "-Djava.awt.headless=true", "-jar", "/app/app.jar"]
