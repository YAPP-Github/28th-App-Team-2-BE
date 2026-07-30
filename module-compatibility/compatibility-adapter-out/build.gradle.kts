plugins {
    id("todakun.adapter-persistence")
}

dependencies {
    implementation(project(":compatibility:domain"))
    implementation(project(":shared"))

    // Spring AI(Vertex AI Gemini) — 궁합 총운 생성. 버전은 BOM 관리, 이 모듈(adapter-out)에서만 사용.
    implementation(platform(libs.spring.ai.bom))
    implementation(libs.spring.ai.starter.model.vertex.ai.gemini)
}
