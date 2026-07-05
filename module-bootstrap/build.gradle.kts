// bootstrap — Spring Boot 진입점. 모든 모듈을 통합하고 웹 계층을 구동한다.
plugins {
    id("todakun.spring-boot")
}

dependencies {
    implementation(project(":common-web"))
    implementation(project(":auth:domain"))
    implementation(project(":auth:adapter-in"))
    implementation(project(":auth:adapter-out"))

    implementation(libs.jackson.module.kotlin)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.sentry.spring.boot.starter.jakarta)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.sentry.spring.boot4.starter)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
}
