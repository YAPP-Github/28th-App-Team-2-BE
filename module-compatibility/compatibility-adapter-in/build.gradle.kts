plugins {
    id("todakun.adapter-web")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":compatibility:domain"))
    implementation(project(":compatibility:application"))
}
