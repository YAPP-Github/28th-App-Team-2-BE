plugins {
    id("todakun.adapter-web")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":daily-fortune:domain"))
    implementation(project(":daily-fortune:application"))
}
