plugins {
    id("todakun.adapter-persistence")
    id("todakun.logging")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":notification:domain"))
    implementation(libs.firebase.admin)
}
