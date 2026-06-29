plugins {
    id("todakun.kotlin-common")
    kotlin("plugin.spring")
}

dependencies {
    // Kotlin+Spring 빈을 쓰는 모든 모듈 공통(프록시·리플렉션). 버전은 Boot BOM 관리.
    implementation(libs.findLibrary("kotlin-reflect").get())
}
