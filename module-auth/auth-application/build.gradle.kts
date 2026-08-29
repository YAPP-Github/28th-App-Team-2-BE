plugins {
    id("todakun.spring")
    id("todakun.logging")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":auth:domain"))
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.slf4j.api)
}
