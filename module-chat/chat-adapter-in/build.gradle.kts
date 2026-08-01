plugins {
    id("todakun.adapter-web")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":chat:domain"))
    implementation(project(":chat:application"))
}
