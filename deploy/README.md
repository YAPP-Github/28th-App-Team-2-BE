# deploy — OKE (Oracle Container Engine for Kubernetes) + Argo CD GitOps 배포

이 디렉터리는 **OKE(Oracle Container Engine for Kubernetes)** 클러스터와 **Argo CD GitOps** 기반 배포를 위한 컨테이너 빌드 자산 및 로컬 개발용 인프라 설정을 포함합니다.

---

## 🏗 아키텍처 개요

```text
[개발자 / CI]
   │
   ├─ push (develop / v*.*.* tag)
   ▼
[GitHub Actions (App Repo)]
   │ 1. ./gradlew check (ktlint / Konsist / Kotest)
   │ 2. ./gradlew :bootstrap:bootJar
   │ 3. Docker build (arm64 native) & push
   │
   ├──────────────────────────────┬──────────────────────────────┐
   ▼                              ▼                              ▼
[GHCR (이미지 레지스트리)]       [GitOps 저장소 (Kustomize)]       [Discord / Sentry]
ghcr.io/<owner>/todakun-app   (overlays/dev, overlays/prod)  실시간 에러 알림 / APM
(dev-${SHA}, X.Y.Z)               │
                                  │ Argo CD GitOps Sync (자동/수동)
                                  ▼
                         [OKE Cluster (aarch64)]
                         ├── Ingress / TLS (Cloudflare)
                         ├── Pod (todakun-app)
                         │     ├── Spring Boot 4.1.0 (JDK 25)
                         │     └── Probes (/actuator/health)
                         └── Database / Redis / External APIs
```

- **앱 저장소 (`28th-App-Team-2-BE`)**: 애플리케이션 소스 코드, 단위/아키텍처 테스트, `Dockerfile`, GitHub Actions 워크플로 관리.
- **GitOps 저장소**: 환경별(`dev`, `prod`) Kubernetes 매니페스트(Kustomize), Sealed Secrets, Ingress, Argo CD Application 설정 관리.
- **컨테이너 레지스트리 (GHCR)**: `ghcr.io/<owner>/todakun-app`에 arm64 네이티브 컨테이너 이미지 저장.
- **배포 오케스트레이션 (Argo CD)**: GitOps 저장소의 선언적 매니페스트를 OKE 클러스터에 반영. 롤백은 GitOps 저장소 커밋 revert (`git revert`)로 수행.

---

## 📁 디렉터리 구성

```text
deploy/
├── Dockerfile                         # JRE 25 런타임 컨테이너 이미지 (arm64 네이티브)
├── .dockerignore                      # 빌드 컨텍스트 최적화 (bootJar만 포함)
├── compose/
│   └── docker-compose.local.yaml      # 로컬 개발용 인프라 (PostgreSQL 17.10 + Redis 7.2)
└── README.md                          # 배포 및 인프라 가이드
```

---

## 🚀 CI/CD 파이프라인

### 1. 개발 환경 배포 (`.github/workflows/deploy-dev.yaml`)

- **트리거**: `develop` 브랜치 푸시 또는 수동 실행 (`workflow_dispatch`)
- **실행 단계**:
  1. **`build` 잡** (`ubuntu-24.04-arm` 러너):
     - JDK 25 및 Gradle 설정
     - `./gradlew check` (코드 스타일·아키텍처 규칙·테스트 검증)
     - `./gradlew :bootstrap:bootJar` 실행 jar 빌드
     - arm64 Docker 이미지 빌드 및 GHCR 푸시 (`dev-${GITHUB_SHA}`, `dev-latest`)
  2. **`bump-manifest` 잡** (`ubuntu-latest` 러너):
     - GitOps 저장소의 `develop` 브랜치 체크아웃 (`GITOPS_REPO_TOKEN` 시크릿 사용)
     - `kustomize edit set image`로 `overlays/dev`의 `todakun-app` 이미지 태그 갱신
     - 매니페스트 커밋 및 푸시 (충돌 시 rebase 최대 3회 재시도)
     - Argo CD가 OKE `dev` 네임스페이스로 변경 사항 자동 반영

### 2. 운영 환경 배포 (`.github/workflows/deploy-prod.yaml`)

- **트리거**: `v*.*.*` 시맨틱 버전 태그 푸시 또는 수동 실행 (`workflow_dispatch`)
- **실행 단계**:
  1. **`build` 잡** (`ubuntu-24.04-arm` 러너):
     - 태그 또는 수동 입력에서 시맨틱 버전(`X.Y.Z`) 추출 및 형식 검증
     - `./gradlew check` 검증 및 bootJar 빌드
     - arm64 Docker 이미지 빌드 및 GHCR 푸시 (`X.Y.Z`, `prod-latest`)
  2. **`bump-manifest` 잡** (`ubuntu-latest` 러너):
     - GitOps 저장소의 `main` 브랜치 체크아웃 (`GITOPS_REPO_TOKEN` 시크릿 사용)
     - `overlays/prod`의 `todakun-app` 이미지 태그 갱신
     - 매니페스트 커밋 및 푸시 (충돌 시 rebase 최대 3회 재시도)
     - Argo CD가 OKE `prod` 네임스페이스로 변경 사항 반영

---

## 🔑 필수 저장소 설정 및 Secrets

| 설정 항목 | 구분 | 설명 |
|-----------|------|------|
| `GITOPS_REPO_TOKEN` | Repository Secret | GitOps 저장소에 `contents: write` 권한을 가진 fine-grained PAT 또는 GitHub App 토큰 (`GITHUB_TOKEN`은 크로스 리포 쓰기 불가) |
| `GITHUB_TOKEN` | Actions 자동 제공 | `packages: write` 권한으로 GHCR에 이미지 푸시 |
| 브랜치 보호 규칙 | GitOps Repo 설정 | GitOps 저장소의 `develop` 및 `main` 브랜치 보호 규칙에서 CI 봇의 매니페스트 푸시 허용(Bypass) 필요 |

---

## 💻 로컬 개발 환경 실행

로컬 개발 시 외부 의존성(DB, Redis)은 `deploy/compose/docker-compose.local.yaml`을 활용해 간편하게 기동할 수 있습니다.

### 1. 인프라 컨테이너 기동

```bash
docker compose -f deploy/compose/docker-compose.local.yaml up -d
```

- **PostgreSQL 17.10**: `localhost:5432` (`todakun` 데이터베이스)
- **Redis 7.2**: `localhost:6379`

### 2. 환경 변수 설정

루트 디렉터리의 `.env.example`을 복사하여 `.env` 파일을 생성하고 필요한 값을 입력합니다:

```bash
cp .env.example .env
```

### 3. 애플리케이션 실행

```bash
./gradlew :bootstrap:bootRun
# 특정 프로필 지정 시:
./gradlew :bootstrap:bootRun --args='--spring.profiles.active=local'
```

---

## 📊 모니터링 및 관측성

- **Sentry (APM & 예외 추적)**: Spring Boot 애플리케이션에 통합되어 에러 트레이스 및 성능 메트릭 수집 (`sentry-logback`).
- **실시간 에러 알림 (Discord)**: 애플리케이션 내장 `DiscordWebhookAppender`가 `ERROR` 로그 발생 시 즉각 Discord 채널로 전송 (`DISCORD_WEBHOOK_URL`).
- **구조화 로깅 (JSON)**: 표준 출력(stdout)으로 Logstash 포맷의 JSON 로그를 출력하며, 클러스터의 로그 수집기(Loki/Vector)가 이를 수집하여 대시보드 조회 및 분석 지원.
- **쿠버네티스 헬스 체크**: Spring Boot Actuator 프로브(`/actuator/health`)를 통해 Pod의 Startup, Readiness, Liveness 상태 관리.
