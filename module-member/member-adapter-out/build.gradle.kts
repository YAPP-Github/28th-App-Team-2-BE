plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":member:domain"))
    implementation(project(":shared"))
    implementation(project(":common-persistence"))
    implementation(libs.spring.boot.starter.data.jpa)

    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.kotest.extensions.spring)
    testRuntimeOnly(libs.postgresql)
}
