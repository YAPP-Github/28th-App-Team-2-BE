// architecture-test — Konsist로 헥사고날 레이어 규칙을 검증한다. 테스트 전용.
plugins {
    id("todakun.kotlin-common")
}

dependencies {
    testImplementation(libs.konsist)

    testImplementation(project(":auth:domain"))
    testImplementation(project(":auth:application"))
    testImplementation(project(":auth:adapter-in"))
    testImplementation(project(":auth:adapter-out"))

    testImplementation(project(":member:domain"))
    testImplementation(project(":member:application"))
    testImplementation(project(":member:adapter-in"))
    testImplementation(project(":member:adapter-out"))

    testImplementation(project(":saju:domain"))
    testImplementation(project(":saju:application"))
    testImplementation(project(":saju:adapter-in"))
    testImplementation(project(":saju:adapter-out"))

    testImplementation(project(":terms:domain"))
    testImplementation(project(":terms:application"))
    testImplementation(project(":terms:adapter-in"))
    testImplementation(project(":terms:adapter-out"))

    testImplementation(project(":luck:domain"))
    testImplementation(project(":luck:application"))
    testImplementation(project(":luck:adapter-in"))
    testImplementation(project(":luck:adapter-out"))

    testImplementation(project(":notification:domain"))
    testImplementation(project(":notification:application"))
    testImplementation(project(":notification:adapter-in"))
    testImplementation(project(":notification:adapter-out"))

    testImplementation(project(":daily-fortune:domain"))
    testImplementation(project(":daily-fortune:application"))
    testImplementation(project(":daily-fortune:adapter-in"))
    testImplementation(project(":daily-fortune:adapter-out"))

    testImplementation(project(":year-fortune:domain"))
    testImplementation(project(":year-fortune:application"))
    testImplementation(project(":year-fortune:adapter-in"))
    testImplementation(project(":year-fortune:adapter-out"))

    testImplementation(project(":compatibility:domain"))
    testImplementation(project(":compatibility:application"))
    testImplementation(project(":compatibility:adapter-in"))
    testImplementation(project(":compatibility:adapter-out"))

    testImplementation(project(":day-fortune:domain"))
    testImplementation(project(":day-fortune:application"))
    testImplementation(project(":day-fortune:adapter-in"))
    testImplementation(project(":day-fortune:adapter-out"))

    testImplementation(project(":chat:domain"))
    testImplementation(project(":chat:application"))
    testImplementation(project(":chat:adapter-in"))
    testImplementation(project(":chat:adapter-out"))
}
