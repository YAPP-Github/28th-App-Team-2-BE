plugins {
    id("todakun.adapter-persistence")
    id("todakun.logging")
}

dependencies {
    implementation(project(":auth:domain"))
    implementation(project(":shared"))
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.spring.web)
    implementation(libs.jackson.annotations)
    implementation(libs.nimbus.jose.jwt)

    testImplementation(libs.bundles.testcontainers.redis)
}

tasks.test {
    // AesGcmStringConverter가 JPA 프로바이더에 의해 직접 생성되어 ENCRYPTION_KEY 환경변수를 직접 읽는다.
    // 테스트 전용 고정 키(AES-256, openssl rand -base64 32로 생성) — 운영 키와 무관하다.
    environment("ENCRYPTION_KEY", "QOZw2OZGGf1rpm4LwEiZz8aHoxjhYpwCVqNSPPgE6YY=")
}
