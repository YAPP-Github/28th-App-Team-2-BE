plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":saju:domain"))
    implementation(libs.spring.context)
}
