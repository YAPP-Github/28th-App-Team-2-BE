plugins {
    id("todakun.adapter-persistence")
}

dependencies {
    implementation(project(":chat:domain"))
    implementation(project(":shared"))

    // Spring AI(Vertex AI Gemini) — 토닥이 스트리밍 답변 생성. 버전은 BOM 관리, 이 모듈(adapter-out)에서만 사용.
    implementation(platform(libs.spring.ai.bom))
    implementation(libs.spring.ai.starter.model.vertex.ai.gemini)

    // 하루 무료 채팅 쿼터 카운터(자정 만료 TTL) 저장에 사용.
    implementation(libs.spring.boot.starter.data.redis)

    // AiResilienceSupport(:common)가 던지는 CallNotPermittedException catch용(이슈 #54).
    implementation(libs.resilience4j.circuitbreaker)

    // 테스트에서 AiResilienceSupport용 레지스트리를 직접 구성하기 위함(회로 open 시나리오 검증).
    testImplementation(libs.resilience4j.retry)
    testImplementation(libs.resilience4j.timelimiter)
}
