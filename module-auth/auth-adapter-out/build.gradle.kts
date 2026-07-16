plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":auth:domain"))
    implementation(project(":shared"))
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.spring.web)
    implementation(libs.jackson.annotations)
    implementation(libs.nimbus.jose.jwt)

    testImplementation(libs.bundles.testcontainers.redis)
    testImplementation(libs.kotest.extensions.spring)
}
