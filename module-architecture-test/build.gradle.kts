// architecture-test — Konsist로 헥사고날 레이어 규칙을 검증한다. 테스트 전용.
plugins {
    id("todakun.kotlin-common")
}

dependencies {
    testImplementation(libs.konsist)
}
