plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":saju:domain"))
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.slf4j.api)
}
