plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":member:domain"))
    implementation(project(":shared"))
    implementation(project(":common-persistence"))
    implementation(libs.spring.boot.starter.data.jpa)
}
