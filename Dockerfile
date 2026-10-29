# Stage 1: build jar
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# Stage 2: runtime (JRE + ffmpeg)
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app appuser \
    && mkdir -p /app/storage/audio \
    && chown -R appuser:appuser /app

WORKDIR /app
COPY --from=build /build/target/tts-api-*.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"
EXPOSE 8010

# Bind mount ./storage/audio do root tạo nên cần chown lúc khởi động, sau đó chạy app bằng user thường
ENTRYPOINT ["sh", "-c", "chown appuser:appuser /app/storage/audio && exec setpriv --reuid=appuser --regid=appuser --init-groups java $JAVA_OPTS -jar /app/app.jar"]
