plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":common-web"))
    implementation(project(":shared"))
    implementation(project(":fortune:domain"))
    implementation(project(":fortune:application"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
}
