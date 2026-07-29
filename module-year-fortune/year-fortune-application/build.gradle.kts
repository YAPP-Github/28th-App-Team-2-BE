plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":year-fortune:domain"))
    implementation(project(":shared"))
}
