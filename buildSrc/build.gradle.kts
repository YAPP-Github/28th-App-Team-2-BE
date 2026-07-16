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
}
