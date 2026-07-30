plugins {
    id("todakun.adapter-web")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":member:domain"))
    implementation(project(":member:application"))
}
