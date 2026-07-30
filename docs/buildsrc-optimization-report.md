# buildSrc 활용 점검 · 최적화 보고서

> 대상: 전 모듈 `build.gradle.kts` 구조 + `buildSrc` 컨벤션 플러그인 활용도
> 작성일: 2026-07-29 · 상태: **적용됨** (이슈 #42 · 브랜치 `chore/#42`) — year-fortune 포함 **도메인 8개**(adapter-in 8 / JPA adapter-out 7) 기준으로 반영 완료. 아래 수치는 최초 분석 시점(도메인 7개) 기준이며 설계는 동일하다.

---

## 1. 요약 진단

| 항목 | 평가 |
|------|------|
| 컨벤션 플러그인 **기반 계층**(kotlin-common → spring → spring-boot, lombok) | ✅ 잘 설계됨 |
| 버전 카탈로그 SSOT(`libs.versions.toml`) + BOM(platform) + 테스트 번들 주입 | ✅ 모범적 |
| `:common` 전역 주입, `archivesName` 경로 고유화, allOpen 커스텀 스테레오타입 | ✅ 의도·주석 명확 |
| **어댑터 계층(adapter-in·adapter-out)의 역할별 보일러플레이트** | ⚠️ **buildSrc로 안 올리고 모듈마다 복붙** |
| 곁다리(중복 `repositories`, 미사용/오배치 `:shared`) | ⚠️ 소소한 정리 여지 |

**한 줄 결론:** 기반은 훌륭하지만, **"인바운드 웹 어댑터"와 "JPA 아웃바운드 어댑터"라는 반복되는 역할**이 컨벤션 플러그인으로 승격되지 않아 어댑터 build 파일 13개에 같은 블록이 복붙돼 있습니다. 역할 플러그인 2개를 추가하면 제거됩니다.

---

## 2. 현재 buildSrc 구조

```
buildSrc/src/main/kotlin/
├── accessors.kt                      # Project.libs (VersionCatalog 런타임 조회 헬퍼)
├── todakun.kotlin-common.gradle.kts  # 모든 모듈 공통: kotlin/jvm·ktlint·JDK25·Boot BOM·kotest/mockk·:common 주입·archivesName
├── todakun.spring.gradle.kts         # kotlin-common + plugin.spring + kotlin-reflect + allOpen(CommandService/QueryService)
├── todakun.spring-boot.gradle.kts    # spring + org.springframework.boot (bootstrap 진입점 전용)
└── todakun.lombok.gradle.kts         # plugin.lombok + lombok compileOnly/annotationProcessor (Java JPA 엔티티 모듈)
```

**잘 하고 있는 점**
- 플러그인이 상속처럼 계층화됨(`spring-boot` → `spring` → `kotlin-common`). 중복 없이 역할이 누적된다.
- precompiled script plugin에서 타입세이프 `libs` 접근자가 없다는 제약을 `accessors.kt`로 우회 — 카탈로그 단일 소스 유지.
- 테스트 공통(kotest 번들·mockk·junit-launcher)을 base 플러그인에서 일괄 주입.
- nested 도메인 모듈 이름 충돌(`:auth:domain` vs `:member:domain`)을 `archivesName` + 루트 `subprojects{ group }`으로 방어.

---

## 3. 개선 포인트 — 어댑터 계층 반복

### 3-1. 인바운드 웹 어댑터 (adapter-in × 7)

`auth·member·saju·terms·luck·daily-fortune·notification` **전부**가 아래 5줄을 동일하게 갖고 있음:

```kotlin
implementation(project(":common-web"))
implementation(libs.spring.boot.starter.web)
implementation(libs.spring.boot.starter.security)
implementation(libs.spring.boot.starter.validation)
implementation(libs.springdoc.openapi.starter.webmvc.ui)
```

→ 5줄 × 7모듈 = **35줄 중복**. 이건 "인바운드 웹 어댑터"라는 **역할의 계약**이므로 플러그인으로 올려야 함.
남는 건 도메인별(`:shared`·`:{domain}:domain`·`:{domain}:application`)뿐.

### 3-2. JPA 아웃바운드 어댑터 (adapter-out × 6)

`member·saju·terms·luck·daily-fortune·notification` **전부**가 아래를 동일하게 가짐:

```kotlin
plugins { id("todakun.spring"); id("todakun.lombok") }   // 플러그인 조합까지 반복
// ...
implementation(project(":common-persistence"))
implementation(libs.spring.boot.starter.data.jpa)
testImplementation(libs.bundles.testcontainers)
testImplementation(libs.kotest.extensions.spring)
testRuntimeOnly(libs.postgresql)
```

→ 플러그인 2줄 + 의존성 5줄이 6모듈에 반복. "JPA 영속성 어댑터" 역할 플러그인으로 승격.
**예외:** `auth-adapter-out`은 Redis + JWT 기반(JPA 미사용)이라 이 플러그인을 적용하지 않고 현행 유지.

### 3-3. 곁다리 정리 (구조 관점)

| 위치 | 문제 | 조치 |
|------|------|------|
| `module-common-web/build.gradle.kts` | `repositories { mavenCentral() }` 블록이 루트 `allprojects` + `kotlin-common`과 중복 | 블록 삭제 |
| `saju-domain` | `implementation(project(":shared"))` 선언했지만 참조 0 | 삭제 |
| `saju-adapter-out` | `implementation(project(":shared"))` 선언했지만 참조 0 | 삭제 |
| `luck-application` | `:shared`가 **테스트에서만** 사용됨(main 참조 0) | `testImplementation`으로 강등 |
| `daily-fortune-adapter-out` | `:shared`가 **테스트에서만** 사용됨(main 참조 0) | `testImplementation`으로 강등 |

> 참고(범위 밖): `architecture-test`는 `Konsist.scopeFromProject()`로 **파일시스템 소스를 직접 파싱**하므로, 선언된 `testImplementation(project(...))` 12개(saju·notification·daily-fortune)는 스캔에 불필요합니다. Konsist는 컴파일 jar가 아닌 `.kt` 소스를 읽기 때문에 auth·member·terms·luck도 이미 검증 대상입니다. → 이 12줄도 정리 가능(별건).

---

## 4. 제안 — 신규 컨벤션 플러그인 2개

### 4-1. `buildSrc/src/main/kotlin/todakun.adapter-web.gradle.kts` (신규)

```kotlin
// 인바운드 웹 어댑터(adapter-in) 공통: REST·보안·검증·Swagger + 응답 엔벌로프(common-web).
// 도메인/애플리케이션/shared 프로젝트 의존성은 도메인마다 달라 각 모듈이 직접 선언한다.
plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":common-web"))
    implementation(libs.findLibrary("spring-boot-starter-web").get())
    implementation(libs.findLibrary("spring-boot-starter-security").get())
    implementation(libs.findLibrary("spring-boot-starter-validation").get())
    implementation(libs.findLibrary("springdoc-openapi-starter-webmvc-ui").get())
}
```

### 4-2. `buildSrc/src/main/kotlin/todakun.adapter-persistence.gradle.kts` (신규)

```kotlin
// 아웃바운드 영속성 어댑터(adapter-out) 공통: JPA + Lombok(Java 엔티티) + Testcontainers 통합테스트.
// 도메인/shared 및 도메인 고유 의존성(spring-ai, firebase 등)은 각 모듈이 직접 선언한다.
plugins {
    id("todakun.spring")
    id("todakun.lombok")
}

dependencies {
    implementation(project(":common-persistence"))
    implementation(libs.findLibrary("spring-boot-starter-data-jpa").get())

    libs.findBundle("testcontainers").get().get().forEach { testImplementation(it) }
    testImplementation(libs.findLibrary("kotest-extensions-spring").get())
    testRuntimeOnly(libs.findLibrary("postgresql").get())
}
```

> 두 플러그인 모두 `todakun.spring`(→ `kotlin-common` → `kotlin("jvm")`)을 적용하므로 `implementation`/`testImplementation` 접근자가 생성됩니다. `project(...)`·`libs.findLibrary/findBundle` 패턴은 기존 `todakun.kotlin-common`·`todakun.lombok`에서 이미 검증된 방식입니다.

---

## 5. 파일별 Before → After

### adapter-in (7개)

**`saju-adapter-in`, `terms-adapter-in`** (`:shared` 미사용):
```kotlin
// Before                                  // After
plugins { id("todakun.spring") }           plugins { id("todakun.adapter-web") }
dependencies {                             dependencies {
    implementation(project(":common-web"))     implementation(project(":saju:domain"))
    implementation(project(":saju:domain"))    implementation(project(":saju:application"))
    implementation(project(":saju:application")) }
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
}
```

**`auth·member·luck·daily-fortune·notification`-adapter-in** (`:shared` 사용):
```kotlin
// After
plugins { id("todakun.adapter-web") }
dependencies {
    implementation(project(":shared"))
    implementation(project(":<domain>:domain"))
    implementation(project(":<domain>:application"))
}
```

### adapter-out (JPA 6개)

| 모듈 | After `dependencies` |
|------|----------------------|
| `member-adapter-out` | `:member:domain`, `:shared` |
| `saju-adapter-out` | `:saju:domain` *(미사용 `:shared` 제거)* |
| `terms-adapter-out` | `:shared`, `:terms:domain` |
| `luck-adapter-out` | `:luck:domain`, `:shared` |
| `daily-fortune-adapter-out` | `:daily-fortune:domain`, `platform(spring-ai-bom)`, `spring-ai-starter-...gemini`, `testImplementation(project(":shared"))` |
| `notification-adapter-out` | `:shared`, `:notification:domain`, `firebase-admin` |

예시 — **`member-adapter-out`**:
```kotlin
// Before                                       // After
plugins {                                       plugins { id("todakun.adapter-persistence") }
    id("todakun.spring")                        dependencies {
    id("todakun.lombok")                            implementation(project(":member:domain"))
}                                                   implementation(project(":shared"))
dependencies {                                  }
    implementation(project(":member:domain"))
    implementation(project(":shared"))
    implementation(project(":common-persistence"))
    implementation(libs.spring.boot.starter.data.jpa)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.kotest.extensions.spring)
    testRuntimeOnly(libs.postgresql)
}
```

예시 — **`daily-fortune-adapter-out`** (도메인 고유 의존성 유지 + `:shared` 테스트 강등):
```kotlin
// After
plugins { id("todakun.adapter-persistence") }
dependencies {
    implementation(project(":daily-fortune:domain"))

    // Spring AI(Vertex AI Gemini) — 이 모듈에서만 사용
    implementation(platform(libs.spring.ai.bom))
    implementation(libs.spring.ai.starter.model.vertex.ai.gemini)

    testImplementation(project(":shared")) // 테스트 픽스처에서만 FortuneCategory 참조
}
```

### auth-adapter-out — **변경 없음** (Redis+JWT, JPA 아님)

### common-web — 중복 `repositories` 삭제
```kotlin
// 파일 하단의 아래 블록 제거 (allprojects + kotlin-common이 이미 제공)
repositories { mavenCentral() }
```

---

## 6. 실행 계획(적용 시)

1. `buildSrc`에 `todakun.adapter-web.gradle.kts`, `todakun.adapter-persistence.gradle.kts` 추가
2. adapter-in 7개 → `id("todakun.adapter-web")` 적용, 중복 5줄 제거
3. JPA adapter-out 6개 → `id("todakun.adapter-persistence")` 적용, 중복 블록 제거, 도메인 고유 의존성만 잔류
4. 곁다리 정리: common-web 중복 `repositories` 삭제, `saju-domain`/`saju-adapter-out` 미사용 `:shared` 삭제, `luck-application`/`daily-fortune-adapter-out` `:shared` → `testImplementation`
5. (선택) `architecture-test`의 불필요한 `testImplementation(project(...))` 12줄 정리
6. **검증:** iCloud `* 2` 중복본 제거 → `./gradlew clean build` (테스트 포함) → `./gradlew :architecture-test:test` → `./gradlew ktlintCheck`

## 7. 리스크 / 주의

- **낮은 리스크·되돌리기 쉬움:** 전부 빌드 설정 변경이며 런타임 코드 무변경. 컴파일/테스트로 즉시 검증 가능.
- **정확도 근거:** 각 모듈 소스의 실제 import를 grep으로 교차검증함(`todakun.shared`·`todakun.web`·`todakun.persistence`). 미사용/테스트전용 판정은 main/test 소스셋을 구분해 확인.
- **주의:** `auth-adapter-out`은 JPA 플러그인 미적용 예외. `daily-fortune-adapter-out`·`notification-adapter-out`은 도메인 고유 의존성(spring-ai/firebase)을 반드시 모듈에 잔류.
- 플러그인 이름(`adapter-web`/`adapter-persistence`)은 팀 컨벤션에 맞게 조정 가능(예: `todakun.web-adapter`, `todakun.jpa-adapter`).