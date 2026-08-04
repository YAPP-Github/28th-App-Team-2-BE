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
}
