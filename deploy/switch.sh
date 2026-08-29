#!/usr/bin/env bash
# 컨테이너 레벨 Blue/Green 전환. GitHub Actions → Ansible이 VM 위에서 실행한다.
#   1) 현재 active color 판별 → idle color 결정
#   2) idle color 기동 후, 일회성 curl 컨테이너로 /actuator/health 폴링(앱 이미지엔 curl 없음)
#   3) Caddy 업스트림을 idle로 교체하고 무중단 reload
#   4) 이전 active color 종료
# 사용법:  APP_IMAGE=<registry>/todakun:<tag> [SPRING_PROFILE=dev|prod] ./switch.sh
#   SPRING_PROFILE 미설정 시 dev로 폴백.
set -euo pipefail
cd "$(dirname "$0")"

: "${APP_IMAGE:?APP_IMAGE 환경변수가 필요합니다 (예: asia-northeast3-docker.pkg.dev/PROJECT/todakun/app:<tag>)}"

APP_COMPOSE="compose/docker-compose.app.yaml"
CADDY_TEMPLATE="caddy/Caddyfile.template"
CADDY_RENDERED="caddy/Caddyfile"
NETWORK="todakun-net"
CURL_IMAGE="${CURL_IMAGE:-curlimages/curl:8.12.1}"
# 소형 VM(스왑 스래싱·버스터블 CPU)에서 앱 부팅이 240초+ 걸리고, 전환 순간엔 green+blue
# 두 JVM이 동시에 떠 더 느려진다. 부팅 중 curl은 connection refused로 즉시 실패(≈4s/회)하므로
# 40회(≈160s)로는 부팅을 못 버텨 롤백됐다 → 여유롭게 늘린다(≈10분 창). VM 증설이 근본 해법.
HEALTH_RETRIES=150
HEALTH_INTERVAL=3

running() { docker ps --format '{{.Names}}' | grep -qx "todakun-app-$1"; }

# 1) active/idle 판별
if running blue; then
  ACTIVE=blue; IDLE=green
elif running green; then
  ACTIVE=green; IDLE=blue
else
  ACTIVE=none; IDLE=blue
fi
echo "[switch] active=$ACTIVE → idle 배포: $IDLE (image=$APP_IMAGE)"

# 2) idle color 기동
# 이미지는 배포 단계(deploy.yaml)가 GHCR 인증(`docker --config`)으로 미리 받아둔다.
# 여기선 로컬 이미지를 그대로 기동한다(--pull never) — switch.sh가 레지스트리 인증에
# 의존하지 않게 해 become 환경변수 전파 이슈를 회피한다.
# SENTRY_RELEASE = 이미지 태그(dev-<git sha>) — Sentry Java SDK가 자동으로 읽어 이슈를 커밋 단위로 묶는다.
# SPRING_PROFILE 미설정 시 dev로 폴백(기존 dev 배포 경로 하위 호환).
COLOR="$IDLE" APP_IMAGE="$APP_IMAGE" SENTRY_RELEASE="${APP_IMAGE##*:}" SPRING_PROFILE="${SPRING_PROFILE:-dev}" \
  docker compose -p "todakun-$IDLE" -f "$APP_COMPOSE" up -d --pull never

# health 대기 — todakun-net 안에서 일회성 curl 컨테이너로 컨테이너명에 직접 요청.
# (앱 이미지에 curl 미포함, 호스트 포트 노출 없음)
echo "[switch] todakun-app-$IDLE healthcheck 대기 (http://todakun-app-$IDLE:8080/actuator/health)..."
healthy=0
for _ in $(seq 1 "$HEALTH_RETRIES"); do
  if docker run --rm --network "$NETWORK" "$CURL_IMAGE" \
      -fsS --connect-timeout 3 --max-time 10 "http://todakun-app-$IDLE:8080/actuator/health" >/dev/null 2>&1; then
    healthy=1; break
  fi
  sleep "$HEALTH_INTERVAL"
done

if [ "$healthy" != "1" ]; then
  echo "[switch] ❌ health 실패 — 롤백(idle 종료), 기존 트래픽 유지"
  docker compose -p "todakun-$IDLE" -f "$APP_COMPOSE" down
  exit 1
fi
echo "[switch] ✅ $IDLE healthy"

# 3) Caddy 업스트림 전환 + 무중단 reload
sed "s|__APP_UPSTREAM__|todakun-app-$IDLE:8080|" "$CADDY_TEMPLATE" > "$CADDY_RENDERED"
docker exec todakun-caddy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
echo "[switch] Caddy → todakun-app-$IDLE 전환 완료"

# 4) 이전 color 종료
if [ "$ACTIVE" != "none" ]; then
  docker compose -p "todakun-$ACTIVE" -f "$APP_COMPOSE" down
  echo "[switch] 이전 color($ACTIVE) 종료"
fi

# 5) 컨테이너 참조가 끊긴 이전 이미지 정리 — 안 지우면 배포할수록 디스크가 계속 줄어든다.
docker image prune -af >/dev/null
echo "[switch] 사용하지 않는 이미지 정리 완료"

echo "[switch] 🎉 배포 완료: $IDLE 활성화"
