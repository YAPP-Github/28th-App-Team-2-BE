plugins {
    id("todakun.kotlin-common")
    kotlin("plugin.spring")
}

dependencies {
    implementation(libs.jackson.annotations)
    implementation(libs.slf4j.api)
    implementation(libs.spring.context) // GlobalExceptionHandler: validation.BindException supertype
    implementation(libs.spring.boot.starter.web) // raw spring-web 대신 Boot 스타터(spring-web/webmvc 제공)

    compileOnly(libs.jakarta.servlet.api) // MissingServletRequestParameterException: ServletException supertype (런타임은 컨테이너 제공)
    implementation(kotlin("stdlib"))
}
repositories {
    mavenCentral()
}
