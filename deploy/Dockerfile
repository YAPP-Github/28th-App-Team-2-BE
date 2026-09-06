# 런타임 이미지: CI에서 `./gradlew :bootstrap:bootJar`로 만든 실행 jar를 담는다.
# 빌드 컨텍스트는 리포지토리 루트(`docker build -f deploy/Dockerfile .`).
# CI는 arm64 러너(ubuntu-24.04-arm)에서 네이티브로 빌드한다 — OKE 노드가 aarch64.
# 참고: plain jar 생성을 끄면(`tasks.named("jar"){ enabled = false }`) 아래 glob이 boot jar 하나만 잡는다.
#
# 헬스 체크용 curl은 넣지 않는다 — Kubernetes의 startup/readiness/liveness probe가
# HTTP GET으로 직접 수행한다.
FROM eclipse-temurin:25-jre

WORKDIR /app
ARG JAR_FILE=module-bootstrap/build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 컨테이너 메모리 limit에 맞춰 힙을 잡는다 — JVM 기본 MaxRAMPercentage(25%)면
# limit 3Gi에서도 힙이 ~768Mi로 묶여 배정한 메모리를 못 쓴다.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Seoul"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
