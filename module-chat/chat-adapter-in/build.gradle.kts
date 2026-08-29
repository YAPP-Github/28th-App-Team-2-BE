plugins {
    id("todakun.adapter-web")
    id("todakun.logging")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":chat:domain"))
    implementation(project(":chat:application"))
}
