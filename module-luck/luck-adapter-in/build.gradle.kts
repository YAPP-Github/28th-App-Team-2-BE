plugins {
    id("todakun.adapter-web")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":luck:domain"))
    implementation(project(":luck:application"))
}
