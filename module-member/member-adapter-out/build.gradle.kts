plugins {
    id("todakun.adapter-persistence")
}

dependencies {
    implementation(project(":member:domain"))
    implementation(project(":shared"))
}
