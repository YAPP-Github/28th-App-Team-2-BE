plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":chat:domain"))
    implementation(project(":shared"))
    implementation(libs.spring.context)
    implementation(libs.slf4j.api)
}
