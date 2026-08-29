plugins {
    id("todakun.adapter-web")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":auth:domain"))
    implementation(project(":auth:application"))
}
