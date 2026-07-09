# Grafana Cloud 설정

VM에는 **Alloy 엣지 에이전트만** 띄우고, Prometheus/Loki/Grafana는 Grafana Cloud 관리형을 쓴다.
Alloy가 metrics를 GC Prometheus로 `remote_write`, logs를 GC Loki로 `push` 한다.

## 1. Grafana Cloud 자격증명 발급

Grafana Cloud 스택 → **Connections**에서 각 엔드포인트/유저/토큰을 확인해 VM `.env`에 채운다.

| .env 변수 | 값 출처 (Grafana Cloud) |
|-----------|--------------------------|
| `GC_PROM_URL` | Prometheus 스택의 remote_write endpoint (`.../api/prom/push`) |
| `GC_PROM_USER` | Prometheus 스택의 username(숫자 ID) |
| `GC_LOKI_URL` | Loki 스택의 push endpoint (`.../loki/api/v1/push`) |
| `GC_LOKI_USER` | Loki 스택의 username(숫자 ID) |
| `GC_TOKEN` | Cloud Access Policy 토큰 (metrics:write, logs:write 스코프) — **유일한 시크릿** |

> `GC_TOKEN` 하나로 Prometheus/Loki 양쪽 basic_auth password를 겸한다.

## 2. ERROR 로그 → Discord 알림

로그가 GC Loki로 가므로 알림도 **Grafana Cloud Alerting**에서 구성한다(로컬 Grafana 없음).

1. Grafana Cloud → **Alerting → Contact points** → Discord 추가, 웹훅 URL 입력 (이름: `discord-errors`).
2. **Alerting → Alert rules** → 새 룰:
   - 데이터소스: **grafanacloud-logs**(Loki)
   - 쿼리: `sum(count_over_time({container=~"todakun-app-.*", level="ERROR"} [5m]))`
   - 조건: `> 0`, `for: 0m`
   - Contact point: `discord-errors`
3. 코드로 관리하려면 [`error-discord-alert.yaml`](./error-discord-alert.yaml)을 Import 스펙/Terraform 기준으로 사용(datasource UID는 GC Loki UID로 교체).

## 3. 보완: Sentry

앱 예외/성능(APM)은 Sentry가 별도 추적한다. Sentry 자체 Discord 통합으로 예외 알림을 이중화할 수 있다.
즉 **로그 레벨 ERROR 알림 = GC Loki 룰 → Discord**, **예외/트랜잭션 = Sentry**로 역할 분리.
