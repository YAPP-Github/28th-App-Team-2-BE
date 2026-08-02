plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":daily-fortune:domain"))
    implementation(project(":shared"))
    implementation(libs.spring.context)
    implementation(libs.slf4j.api)

    // Spring Batch — 오늘의 운세 생성 배치(Job/Step/Chunk). 버전은 Boot BOM 관리.
    implementation(libs.spring.boot.starter.batch)
}
