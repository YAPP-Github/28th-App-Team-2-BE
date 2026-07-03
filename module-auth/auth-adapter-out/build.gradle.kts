plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":auth:domain"))
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
}
