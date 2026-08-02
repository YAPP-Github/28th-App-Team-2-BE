plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":auth:domain"))
    implementation(libs.spring.context)
}
