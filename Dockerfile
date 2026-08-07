# ---------- 1단계: 빌드 ----------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Gradle wrapper와 설정 파일만 먼저 복사 (의존성 캐시 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# non-root 사용자로 실행 (보안 권장)
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
