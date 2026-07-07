# deploy — GCP VM 배포 (Caddy + Docker Compose Blue/Green)

`todakun-instance`(단일 GCP VM) 위에서 **컨테이너 레벨 Blue/Green**으로 무중단 앱 배포를 한다.
DB는 Cloud SQL(`todakun-database`, 외부), 파일은 Cloud Storage(`todakun-bucket`), Redis는 VM 내부 컨테이너.

## 구성

```
deploy/
├── Dockerfile                       # 실행 jar 런타임 이미지 (curl 미포함, build 컨텍스트 = 리포 루트)
├── compose/
│   ├── docker-compose.infra.yaml     # 상시: Caddy + Redis (todakun-net)
│   └── docker-compose.app.yaml       # 앱 color 1개 (COLOR/APP_IMAGE 파라미터, 포트 비노출)
├── caddy/
│   └── Caddyfile.template           # __APP_UPSTREAM__ → switch.sh가 active color로 치환
├── switch.sh                        # Blue/Green 전환 (일회성 curl 컨테이너로 헬스 체크)
├── ansible/
│   ├── ansible.cfg                  # sops vars 플러그인 활성화
│   ├── .sops.yaml                   # SOPS 암호화 규칙(age recipient)
│   ├── SECRETS.md                   # 🔑 SOPS+age 시크릿 관리 런북
│   ├── collections/requirements.yaml # community.docker / community.sops
│   ├── inventory/hosts.ini.example  # VM 접속 정보 템플릿(dev/prod 공용, 루트에 둠 — 파싱 대상 밖)
│   ├── inventory/{dev,prod}/hosts.ini  # 환경별 접속 정보 (커밋 X, CI는 secrets로 렌더)
│   ├── inventory/{dev,prod}/group_vars/all/secrets.sops.{dev,prod}.yaml  # 🔐 암호화 시크릿(커밋 O) — .env 소스
│   ├── templates/env.j2             # secrets → /opt/todakun/.env 렌더 템플릿
│   ├── tasks/render_env.yaml         # .env 렌더 태스크
│   ├── playbook.yaml                 # VM 프로비저닝 (Docker/Caddy/Redis/네트워크/AR 인증/Alloy/.env)
│   └── deploy.yaml                   # Blue/Green 배포 (.env 갱신 + switch.sh 실행)
├── monitoring/
│   ├── docker-compose.monitoring.yaml  # Alloy 엣지 에이전트(단일)
│   ├── alloy/config.alloy              # host+actuator 메트릭 → GC Prometheus, Docker 로그 → GC Loki
│   └── grafana-cloud/                  # GC 설정 가이드 + Discord ERROR 알림 룰 스펙
└── .env.example                     # 앱 런타임 시크릿 + GC 자격증명 (VM의 .env로 복사)

.github/workflows/deploy-dev.yaml     # CI: 빌드 → 이미지 푸시 → Ansible 프로비저닝 + 배포
```

## 역할 분리

- **CI (GitHub Actions)**: jar 빌드 → 이미지 빌드/푸시(Artifact Registry) → Ansible 실행.
- **프로비저닝 (Ansible `playbook.yaml`, 멱등)**: Docker·compose 설치, 파일 배치, `todakun-net` 생성, GHCR 로그인, `.env`·인증서 렌더링, 공용 인프라 기동.
- **배포 (Ansible `deploy.yaml` → `switch.sh`, 매 릴리스)**: 최신 산출물 동기화 후 Blue/Green 전환.

## 헬스 체크 (curl은 컨테이너 밖)

앱 이미지에는 **curl을 넣지 않고**, 호스트 포트도 노출하지 않는다. `switch.sh`가 **일회성 curl 컨테이너**(`curlimages/curl`)를 `todakun-net` 안에서 실행해 컨테이너명으로 직접 요청한다:

```bash
docker run --rm --network todakun-net curlimages/curl:8.12.1 -fsS http://todakun-app-<idle>:8080/actuator/health
```

즉 헬스 체크는 앱 컨테이너가 아니라 배포 파이프라인(GitHub Actions → Ansible → switch.sh)이 수행한다. Caddy의 액티브 헬스 체크(`health_uri`)는 런타임 트래픽 안전망으로 별도 동작.

## 시크릿 (SOPS) — VM `.env`는 수동 배치 안 함

시크릿은 `ansible/inventory/group_vars/all/secrets.sops.yaml`(SOPS 암호화, 커밋)에 두고, Ansible이
컨트롤러에서 복호화해 VM `/opt/todakun/.env`로 렌더링한다. 최초 셋업(age 키 생성·암호화·CI 연결)은 **[`ansible/SECRETS.md`](./ansible/SECRETS.md)** 참고.
`deploy/.env.example`은 이제 스키마 문서 + 로컬 `bootRun`용이며, VM에는 사용하지 않는다.

## 최초 세팅 (수동)

```bash
cd deploy/ansible
cp inventory/hosts.ini.example inventory/dev/hosts.ini          # <env>→dev, VM IP/유저 채우기
# SECRETS.md대로 age 키 생성 → secrets.sops.dev.yaml 작성·암호화
export SOPS_AGE_KEY=...                                         # 또는 ~/.config/sops/age/keys.txt
ansible-playbook -i inventory/dev/hosts.ini playbook.yaml       # .env는 SOPS에서 자동 렌더
```

## CI 배포 (GitHub Actions)

`develop` 푸시 또는 수동 실행(`workflow_dispatch`) 시 `.github/workflows/deploy-dev.yaml`이:
**build**(`./gradlew check` 테스트 → bootJar → **GHCR 이미지 푸시**) → **deploy**(`playbook.yaml` 프로비저닝 → `deploy.yaml` Blue/Green)을 실행한다.
`concurrency: deploy-dev`로 동시 배포는 직렬화된다. 이미지는 **GHCR**(`ghcr.io/<owner>/todakun-app`)를 쓰고, CI 푸시는 자동 제공되는 `GITHUB_TOKEN`(`packages: write`)으로 한다 — **GCP 인증(WIF) 불필요.**

**필요한 GitHub Secrets**

| Secret | 용도 |
|--------|------|
| `VM_HOST` | VM 외부 IP/호스트 |
| `VM_USER` | VM SSH 유저 |
| `VM_SSH_PRIVATE_KEY` | VM 접속용 SSH 개인키 |
| `SOPS_AGE_KEY` | SOPS 복호화용 age 개인키 (`AGE-SECRET-KEY-1...`) |

> GHCR **push**는 `GITHUB_TOKEN` 자동 처리. VM의 **pull**은 private 이미지라 SOPS 시크릿의 `ghcr_user`/`ghcr_pat`(read:packages PAT)로 `docker login`(playbook이 수행).

`switch.sh` 동작: active color 판별 → idle color 기동 → 일회성 curl 컨테이너로 `/actuator/health` healthy 대기 →
Caddy 업스트림 교체 후 무중단 `caddy reload` → 이전 color 종료. **health 실패 시 idle을 내리고 기존 트래픽 유지(자동 롤백).**

## 모니터링 / 알림 (Grafana Cloud + Alloy 엣지)

VM에는 **Alloy 엣지 에이전트만** 띄우고, Prometheus/Loki/Grafana는 **Grafana Cloud 관리형**을 쓴다(소형 VM 메모리 절약). 자세한 설정은 [`monitoring/grafana-cloud/README.md`](./monitoring/grafana-cloud/README.md).

| 구성 | 역할 |
|------|------|
| **Alloy** (VM) | host 메트릭(node_exporter 대체) + 앱 `/actuator/prometheus`(blue/green) 스크레이프 → **GC Prometheus** remote_write / Docker 로그 JSON `level` 파싱 → **GC Loki** |
| **Grafana Cloud** | 관리형 Prometheus/Loki + 대시보드 + **Discord ERROR 알림** (Alerting) |
| **Sentry** | 앱 예외/성능 추적(APM). SDK가 `SENTRY_DSN`으로 전송 |

**로그 알림 흐름**: 앱이 `logstash` 구조화 로그(JSON) stdout 출력 → Alloy가 `level` 라벨 부여 → GC Loki 적재 →
GC Alerting 룰이 `{container=~"todakun-app-.*", level="ERROR"}` 5분 카운트 > 0 감지 → **Discord 웹훅**.

**필요 env**(VM `.env`): `SENTRY_DSN`, `GC_TOKEN`, `GC_PROM_URL`, `GC_PROM_USER`, `GC_LOKI_URL`, `GC_LOKI_USER`.
Discord 웹훅은 Grafana Cloud Alerting의 Contact point에 직접 등록한다(`.env` 아님).

> Alloy 이미지 태그(`grafana/alloy:v1.16.1`)와 GC 엔드포인트는 배포 시점 기준으로 확인. 알림 룰은 GC UI에서 로드/발화 확인.

## 도메인 / HTTPS (Cloudflare, 은닉 방식)

개발 서버 API는 **비밀 랜덤 서브도메인**(`{$DOMAIN}`, 예: `k9x2m7q3.todakun.com`)으로 받는다. DNS·TLS는 **Cloudflare**를 통한다.

- 사이트 주소는 `Caddyfile.template`에 하드코딩하지 않고 **VM `.env`의 `DOMAIN`으로 주입**한다 — 도메인명을 git에 남기지 않기 위함.
- **Cloudflare 프록시(orange)** + **Full (Strict)**. 클라이언트↔Cloudflare, Cloudflare↔origin 모두 암호화, origin IP 은닉.
- origin(Caddy)에는 **와일드카드 Origin Certificate**(`*.todakun.com`)를 쓴다. **SOPS에 넣으면 Ansible이 VM `caddy/certs/`로 렌더링**(수동 배치 X, [가이드](./caddy/certs/README.md)). Caddy는 ACME를 쓰지 않는다.
- 엣지는 **Universal SSL 와일드카드**라 특정 서브도메인이 **CT 로그에 남지 않는다** → 비밀 이름 + 와일드카드 인증서 조합이 은닉의 핵심.

**전제 조건**
1. **Cloudflare DNS**: 랜덤 서브도메인 레코드 → VM IP, **프록시 ON(orange)**.
2. SOPS 시크릿에 `domain`(랜덤) + 와일드카드 Origin 인증서(`cloudflare_origin_cert`/`key`) 등록 → Ansible이 렌더링.
3. 그 특정 서브도메인 앞으로 **dedicated/advanced 인증서를 발급하지 말 것**(발급하면 CT 로그에 이름이 찍힘).

## ⚠️ 개발 서버 노출 정책 — 은닉의 한계

접근 통제(Cloudflare Access / Tunnel)를 당장 못 쓰는 상황이라 **도메인 은닉(obscurity)** 으로 노출을 줄인다. **이것은 임시 방편이며 접근 통제의 대체가 아니다.**

- **성립 조건**: 도메인명이 코드/커밋/Referer/스크린샷/공유 문서/브라우저 히스토리 등 **어디에도 새지 않아야** 한다. 한 번 새면 무력화된다.
- **actuator는 Caddy에서 외부 404**(`/actuator/*`) — 메트릭/health 비공개(내부 스크레이프는 영향 없음).
- **Swagger**(dev `/swagger-ui.html` 오픈)는 시크릿 경로로 돌리는 것을 권장.
- **API 자체 인증**(JWT/OAuth)이 실제 방어선 — 은닉은 스캔 노이즈를 줄일 뿐이다.
- **여건이 되면 즉시 Cloudflare Access + Tunnel로 전환** 할 것(도메인 알려져도 안전 + IP·인증서 관리 불필요).

## 인증 (GCP)

VM에 붙은 서비스 계정의 **ADC**로 Cloud Storage에 인증한다(키 파일/시크릿 불필요).
서비스 계정에 `roles/storage.objectAdmin`(버킷 범위)과 Cloud SQL 접근 권한을 부여할 것.

## 주의 (운영 승격 전 체크)

- **DB 마이그레이션은 backward-compatible(expand/contract)** 여야 한다 — blue/green 두 color가 같은 Cloud SQL을 공유. 스키마 변경 관리를 위해 **Flyway/Liquibase 도입 권장**(현재 `ddl-auto: validate` 전제).
- **Redis는 두 color가 공유**한다(color별로 띄우지 않음). AOF 지속성으로 재시작 시 refresh token 보존.
- 전환 순간 blue+green 앱이 공존 → **VM 메모리를 앱 2배 순간 점유** 기준으로 사이징.
- 단일 VM이므로 **VM 장애에 대한 무중단은 보장하지 않는다**(앱 배포 무중단만).
- dev는 `.env` 주입으로 충분. 운영 승격 시 **GCP Secret Manager** 고려.