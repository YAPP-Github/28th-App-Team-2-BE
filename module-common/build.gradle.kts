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
}
