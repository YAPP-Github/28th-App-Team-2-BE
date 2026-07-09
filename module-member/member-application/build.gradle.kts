plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":member:domain"))
    implementation(project(":shared"))
}
