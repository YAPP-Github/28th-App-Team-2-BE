# todakun (토닥운) — 백엔드

YAPP 28기 App 2팀 백엔드 애플리케이션 서버입니다. (AOS / iOS 클라이언트 대상)

도메인 주도 설계(DDD) 기반 헥사고날 아키텍처 및 멀티 모듈 구조로 설계되었습니다.

---

## 🛠 Tech Stack

| 분류 | 기술 스택 |
|------|-----------|
| **Application** | Java 25 / Kotlin 2.3.21 / Spring Boot 4.1.0 / Gradle 9.5.1 |
| **Database & Cache** | PostgreSQL 17.10 (Hibernate JPA, pgvector) / Redis 7.2 |
| **AI Integration** | Spring AI / Google Vertex AI (Gemini) |
| **Push Notification** | Firebase Cloud Messaging (Firebase Admin SDK, ADC) |
| **Infrastructure** | Oracle Cloud Infrastructure (OCI) OKE (Kubernetes) / Argo CD GitOps |
| **Testing & Lint** | Kotest / MockK / Testcontainers / Ktlint / Konsist |

---

## 📁 모듈 구조

```text
todakun/
├── module-bootstrap/                 # (:bootstrap) Spring Boot 엔트리포인트, Security/로깅 설정
├── module-common/                    # (:common) AppException, ResponseCode, @CommandService/@QueryService
├── module-common-web/                # (:common-web) CommonResponse, GlobalExceptionHandler, @DisableSwaggerSecurity
├── module-shared/                    # (:shared) 도메인 간 공유 인터페이스 및 식별자 (UserId, OAuthProvider 등)
├── module-{domain}/                  # (:{domain} 컨테이너) auth, member, saju, daily-fortune, ...
│   ├── {domain}-domain/              # (:{domain}:domain) 순수 Kotlin 엔티티 및 포트 인터페이스
│   ├── {domain}-application/         # (:{domain}:application) UseCase 유스케이스 서비스 (@CommandService/@QueryService)
│   ├── {domain}-adapter-in/          # (:{domain}:adapter-in) REST Controller, DTO, Swagger Api
│   └── {domain}-adapter-out/         # (:{domain}:adapter-out) JPA(Java *JpaEntity), Redis, AI, 외부 어댑터
├── module-architecture-test/         # (:architecture-test) Konsist 아키텍처 규칙 검증 테스트
└── deploy/                           # 배포 자산 (Dockerfile, 로컬 개발용 compose)
```

---

## 💻 로컬 개발 환경 실행

### 1. 인프라 컨테이너 기동

```bash
docker compose -f deploy/compose/docker-compose.local.yaml up -d
```

### 2. 환경 변수 설정

```bash
cp .env.example .env
# .env 파일에 필요한 설정값 입력
```

### 3. 애플리케이션 실행

```bash
./gradlew :bootstrap:bootRun
# local 프로필 명시 시:
./gradlew :bootstrap:bootRun --args='--spring.profiles.active=local'
```

---

## 🚀 배포 및 CI/CD

배포 및 클러스터 아키텍처 관련 상세 내용은 [**`deploy/README.md`**](deploy/README.md)를 참고하세요.

- **CI/CD**: GitHub Actions 워크플로 (`deploy-dev.yaml`, `deploy-prod.yaml`)를 통해 GHCR 이미지 빌드 및 GitOps 저장소 매니페스트 태그 갱신
- **CD**: Argo CD가 GitOps 저장소의 선언적 매니페스트(Kustomize)를 OKE 클러스터에 자동 동기화
