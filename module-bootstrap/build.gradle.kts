// bootstrap — Spring Boot 진입점. 모든 모듈을 통합하고 웹 계층을 구동한다.
plugins {
    id("todakun.spring-boot")
}

dependencies {
    implementation(project(":common-web"))

    implementation(libs.jackson.module.kotlin)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
}
