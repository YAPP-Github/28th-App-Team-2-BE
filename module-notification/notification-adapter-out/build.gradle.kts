plugins {
    id("todakun.adapter-persistence")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":notification:domain"))
    implementation(libs.firebase.admin)
}
