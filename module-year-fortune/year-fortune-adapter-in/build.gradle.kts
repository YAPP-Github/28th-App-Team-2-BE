plugins {
    id("todakun.adapter-web")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":year-fortune:domain"))
    implementation(project(":year-fortune:application"))
}
