plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.allopen) // kotlin("plugin.spring")
    implementation(libs.kotlin.lombok.gradle.plugin) // kotlin("plugin.lombok")
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.ksp.gradle.plugin) // id("com.google.devtools.ksp"): todakun.logging 컨벤션 플러그인 전용
}
