plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":compatibility:domain"))
    implementation(project(":shared"))
    implementation(libs.spring.context)
    implementation(libs.slf4j.api)
}
