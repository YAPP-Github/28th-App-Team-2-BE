plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":common-web"))
    implementation(project(":auth:domain"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
}
