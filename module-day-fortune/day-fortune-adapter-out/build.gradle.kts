plugins {
    id("todakun.spring")
    id("todakun.lombok")
}

dependencies {
    implementation(project(":day-fortune:domain"))
    implementation(project(":shared"))
    implementation(project(":common-persistence"))
    implementation(libs.spring.boot.starter.data.jpa)

    // Spring AI(Vertex AI Gemini) — 택일 운세 생성. 버전은 BOM 관리, 이 모듈(adapter-out)에서만 사용.
    implementation(platform(libs.spring.ai.bom))
    implementation(libs.spring.ai.starter.model.vertex.ai.gemini)

    // AiResilienceSupport(:common)가 던지는 CallNotPermittedException catch용(이슈 #54).
    implementation(libs.resilience4j.circuitbreaker)

    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.kotest.extensions.spring)
    testRuntimeOnly(libs.postgresql)

    // 테스트에서 AiResilienceSupport용 레지스트리를 직접 구성하기 위함(회로 open/Retry/TimeLimiter 시나리오 검증).
    testImplementation(libs.resilience4j.retry)
    testImplementation(libs.resilience4j.timelimiter)
}
