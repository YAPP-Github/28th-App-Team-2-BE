plugins {
    id("todakun.spring")
    id("todakun.lombok")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":notification:domain"))
    implementation(project(":common-persistence"))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.firebase.admin)

    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.kotest.extensions.spring)
    testRuntimeOnly(libs.postgresql)
}
