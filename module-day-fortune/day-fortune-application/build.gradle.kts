plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":day-fortune:domain"))
    implementation(project(":shared"))
    implementation(libs.spring.context)
    implementation(libs.slf4j.api)
}
