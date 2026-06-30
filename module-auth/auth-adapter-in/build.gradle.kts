plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":common-web"))
    implementation(project(":auth-application"))
    implementation(project(":auth-domain"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
}
