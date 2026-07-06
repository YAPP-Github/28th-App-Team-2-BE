#!/usr/bin/env bash
# 컨테이너 레벨 Blue/Green 전환. GitHub Actions → Ansible이 VM 위에서 실행한다.
#   1) 현재 active color 판별 → idle color 결정
#   2) idle color 기동 후, 일회성 curl 컨테이너로 /actuator/health 폴링(앱 이미지엔 curl 없음)
#   3) Caddy 업스트림을 idle로 교체하고 무중단 reload
#   4) 이전 active color 종료
# 사용법:  APP_IMAGE=<registry>/todakun:<tag> ./switch.sh
set -euo pipefail
cd "$(dirname "$0")"

: "${APP_IMAGE:?APP_IMAGE 환경변수가 필요합니다 (예: asia-northeast3-docker.pkg.dev/PROJECT/todakun/app:<tag>)}"

APP_COMPOSE="compose/docker-compose.app.yml"
CADDY_TEMPLATE="caddy/Caddyfile.template"
CADDY_RENDERED="caddy/Caddyfile"
NETWORK="todakun-net"
CURL_IMAGE="${CURL_IMAGE:-curlimages/curl:8.12.1}"
HEALTH_RETRIES=40
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
COLOR="$IDLE" APP_IMAGE="$APP_IMAGE" \
  docker compose -p "todakun-$IDLE" -f "$APP_COMPOSE" up -d --pull always

# health 대기 — todakun-net 안에서 일회성 curl 컨테이너로 컨테이너명에 직접 요청.
# (앱 이미지에 curl 미포함, 호스트 포트 노출 없음 — BEAT 패턴)
echo "[switch] todakun-app-$IDLE healthcheck 대기 (http://todakun-app-$IDLE:8080/actuator/health)..."
healthy=0
for _ in $(seq 1 "$HEALTH_RETRIES"); do
  if docker run --rm --network "$NETWORK" "$CURL_IMAGE" \
      -fsS --connect-timeout 3 --max-time 5 "http://todakun-app-$IDLE:8080/actuator/health" >/dev/null 2>&1; then
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

echo "[switch] 🎉 배포 완료: $IDLE 활성화"
