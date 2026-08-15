plugins {
    id("todakun.adapter-persistence")
}

dependencies {
    implementation(project(":daily-fortune:domain"))
    implementation(project(":shared"))

    // Spring AI(Vertex AI Gemini) — 오늘의 운세 생성. 버전은 BOM 관리, 이 모듈(adapter-out)에서만 사용.
    implementation(platform(libs.spring.ai.bom))
    implementation(libs.spring.ai.starter.model.vertex.ai.gemini)

    // spring-ai BeanOutputConverter가 내부적으로 생성하는 Jackson 2 ObjectMapper에 Kotlin data class 지원을 등록하기 위함.
    runtimeOnly(libs.jackson.module.kotlin.jackson2)

    // AiResilienceSupport(:common)가 던지는 CallNotPermittedException catch용(이슈 #54).
    implementation(libs.resilience4j.circuitbreaker)

    // 테스트에서 AiResilienceSupport용 레지스트리를 직접 구성하기 위함(회로 open/TimeLimiter 타임아웃 시나리오 검증).
    testImplementation(libs.resilience4j.retry)
    testImplementation(libs.resilience4j.timelimiter)
}
