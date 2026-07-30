// 인바운드 웹 어댑터(adapter-in) 공통: REST·보안·검증·Swagger + 응답 엔벌로프(common-web).
// 도메인/애플리케이션/shared 프로젝트 의존성은 도메인마다 달라 각 모듈이 직접 선언한다.
plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":common-web"))
    implementation(libs.findLibrary("spring-boot-starter-web").get())
    implementation(libs.findLibrary("spring-boot-starter-security").get())
    implementation(libs.findLibrary("spring-boot-starter-validation").get())
    implementation(libs.findLibrary("springdoc-openapi-starter-webmvc-ui").get())
}
