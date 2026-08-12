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

## 2. Grafana Cloud의 역할 = 조회·대시보드 (알림 아님)

GC로 보낸 metrics/logs는 **대시보드 조회와 사후 분석용**이다.
**GC Alerting은 쓰지 않는다** — ERROR 알림은 아래 두 경로가 전담한다.

| 용도 | 담당 |
|------|------|
| ERROR 발생 시 **즉시 Discord 알림** | 앱 내장 logback appender(`DiscordWebhookAppender`) |
| 예외 **이슈 추적·트리아지** | Sentry (`sentry-logback`) |
| metrics/logs **조회·대시보드** | Grafana Cloud (Prometheus / Loki) |

### ERROR 로그 → Discord (앱이 직접 발송)

ERROR 로그가 발생하면 애플리케이션 프로세스가 그 자리에서 직접 Discord 웹훅으로 POST한다 —
로거 이름·스레드·메시지·스택 트레이스를 지연 없이 포함해서 보낸다.
`DISCORD_WEBHOOK_URL` 환경변수 하나로 켜고 끈다(비우면 워커 스레드조차 뜨지 않는 완전 no-op).

> Loki 로그 기반 알림 룰(약 5분 지연)을 GC Alerting에 두는 방식은 **채택하지 않았다.**
> 지연이 크고 룰 관리 비용이 별도로 드는 데다, 앱 직접 발송과 병행하면 같은 에러가 두 번 온다.
> 따라서 **GC Alerting에는 ERROR 로그 알림 룰을 만들지 않는다.**
>
> ⚠️ 과거에 GC UI에 `todakun-error-logs` 룰을 만들어 뒀다면 **수동으로 삭제/비활성화**해야 한다.
> 코드에는 해당 룰 스펙이 더 이상 없으므로(제거됨), UI에 남아 있으면 중복 알림의 원인이 된다.

## 3. 보완: Sentry

앱 예외/성능(APM)은 Sentry가 별도 추적한다. `sentry-logback`이 클래스패스에 있어 ERROR 로그가
Sentry 이슈로도 함께 적재된다(`sentry.logging.minimum-event-level=error`).

- **즉시 알림(Discord)** = 앱 내장 logback appender
- **이슈 추적/트리아지(Sentry)** = 로그 기반 이슈 적재 + 예외/트랜잭션 APM

Sentry 자체 Discord 통합(Alerts → Integrations)을 추가로 켜면 같은 에러가 Discord에 두 번 오므로,
켜려면 앱 내장 appender와의 중복을 감안할 것.
