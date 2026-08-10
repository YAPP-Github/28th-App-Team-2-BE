plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":terms:domain"))
    implementation(libs.spring.context)
}
