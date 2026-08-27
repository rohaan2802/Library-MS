FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/uploads/profile-pictures /app/backup \
    && chown -R app:app /app
COPY --from=build /app/target/*.jar /app/app.jar
# Seed existing profile photos from the local lab (DB paths like /uploads/profiles/3.jpg)
COPY seed-uploads/ /app/uploads/profile-pictures/
RUN chown -R app:app /app/uploads
ENV APP_BROWSER_OPEN_ON_START=false \
    SPRING_PROFILES_ACTIVE=cloud \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=50.0 -XX:+UseSerialGC -Xss256k -XX:MaxMetaspaceSize=80m -XX:CompressedClassSpaceSize=32m -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
