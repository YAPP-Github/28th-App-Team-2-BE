# 런타임 이미지: CI에서 `./gradlew :bootstrap:bootJar`로 만든 실행 jar를 담는다.
# 빌드 컨텍스트는 리포지토리 루트(`docker build -f deploy/Dockerfile .`).
# 참고: plain jar 생성을 끄면(`tasks.named("jar"){ enabled = false }`) 아래 glob이 boot jar 하나만 잡는다.
#
# curl은 설치하지 않는다 — Blue/Green 헬스 체크는 컨테이너가 아니라
# 배포 파이프라인(GitHub Actions → Ansible → switch.sh)이 VM 호스트 curl로 수행한다.
FROM eclipse-temurin:25-jre

WORKDIR /app
ARG JAR_FILE=module-bootstrap/build/libs/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]