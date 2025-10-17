FROM openjdk:17-jdk-slim

WORKDIR /app

# Gradle Wrapper 파일 복사
COPY gradlew /app/gradlew
COPY gradle /app/gradle
RUN chmod +x ./gradlew

# 빌드 설정 파일 복사
COPY build.gradle /app/build.gradle
COPY settings.gradle /app/settings.gradle

# 의존성 미리 다운로드
RUN ./gradlew build --no-daemon --parallel --continue --stacktrace -x test || true

# 전체 소스 코드 복사
COPY . .

# 애플리케이션 빌드
RUN ./gradlew build -x test --no-daemon

# 빌드된 JAR 파일을 실행 가능한 위치로 복사 (plain JAR 제외)
RUN find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} app.jar \;

# 포트 노출 (image-service의 기본 포트)
EXPOSE 8084

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]