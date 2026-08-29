plugins {
    id("todakun.adapter-web")
}

dependencies {
    implementation(project(":terms:domain"))
    implementation(project(":terms:application"))
}
