plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":daily-fortune:domain"))
    implementation(project(":shared"))
}
