plugins {
    id("todakun.adapter-web")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":notification:domain"))
    implementation(project(":notification:application"))
}
