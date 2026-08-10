plugins {
    id("todakun.spring")
    id("todakun.logging")
}

dependencies {
    implementation(project(":year-fortune:domain"))
    implementation(project(":shared"))
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.slf4j.api)
}
