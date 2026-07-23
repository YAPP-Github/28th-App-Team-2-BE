// architecture-test — Konsist로 헥사고날 레이어 규칙을 검증한다. 테스트 전용.
plugins {
    id("todakun.kotlin-common")
}

dependencies {
    testImplementation(libs.konsist)

    testImplementation(project(":saju:domain"))
    testImplementation(project(":saju:application"))
    testImplementation(project(":saju:adapter-in"))
    testImplementation(project(":saju:adapter-out"))

    testImplementation(project(":notification:domain"))
    testImplementation(project(":notification:application"))
    testImplementation(project(":notification:adapter-in"))
    testImplementation(project(":notification:adapter-out"))

    testImplementation(project(":fortune:domain"))
    testImplementation(project(":fortune:application"))
    testImplementation(project(":fortune:adapter-in"))
    testImplementation(project(":fortune:adapter-out"))
}
