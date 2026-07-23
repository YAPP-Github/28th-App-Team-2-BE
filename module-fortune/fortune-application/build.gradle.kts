plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":fortune:domain"))
    implementation(project(":shared"))
}
