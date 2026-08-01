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
}
