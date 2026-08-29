plugins {
    id("todakun.kotlin-common")
}

dependencies {
    implementation(libs.spring.context)
    implementation(libs.spring.tx)

    // AiResilienceSupport(도메인 공통 CircuitBreaker/Retry/TimeLimiter 실행기) 구현에 사용.
    // 레지스트리 등록·메트릭 바인딩은 bootstrap의 AiResilienceConfig가 담당하므로 여기선 core API만 필요.
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.timelimiter)

    // vertexResponseSchema(BeanOutputConverter의 소문자 JSON Schema를 Vertex Schema proto가 인식하는 대문자로 변환)에 사용.
    // spring-ai-model은 provider-agnostic 코어라 Vertex 전용 스타터 없이 BeanOutputConverter/ModelOptionsUtils만 가져온다.
    implementation(platform(libs.spring.ai.bom))
    implementation(libs.spring.ai.model)

    // BeanOutputConverter가 내부적으로 생성하는 Jackson 2 ObjectMapper에 Kotlin data class 지원을 등록하기 위함.
    runtimeOnly(libs.jackson.module.kotlin.jackson2)
}
