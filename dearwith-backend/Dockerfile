# 1) Build: Gradle로 JAR 파일 생성
FROM gradle:7.5-jdk17 AS builder
WORKDIR /app

# 캐시 최적화: build 스크립트만 먼저 복사
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

# 소스 복사 및 빌드
COPY src src
RUN ./gradlew clean bootJar -x test

# 2) Runtime: 경량 JRE 이미지에서 실행
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 빌드 산출물만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 포트
EXPOSE 8080

# 컨테이너 시작 커맨드
ENTRYPOINT ["java","-jar","app.jar"]