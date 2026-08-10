plugins {
    id("todakun.kotlin-common")
    kotlin("plugin.spring")
    id("todakun.logging")
}

dependencies {
    implementation(libs.jackson.annotations)
    implementation(libs.slf4j.api)
    implementation(libs.spring.context) // GlobalExceptionHandler: validation.BindException supertype
    implementation(libs.spring.boot.starter.web) // raw spring-web 대신 Boot 스타터(spring-web/webmvc 제공)

    compileOnly(libs.jakarta.servlet.api) // MissingServletRequestParameterException: ServletException supertype (런타임은 컨테이너 제공)
    compileOnly(libs.jackson.module.kotlin) // KotlinInvalidNullException: 필수 파라미터 누락 판별 (런타임은 bootstrap이 제공)
    compileOnly(
        libs.spring.boot.starter.validation,
    ) // ValidEnum: jakarta.validation Constraint/ConstraintValidator 계약 (런타임은 adapter-in이 제공)
    implementation(kotlin("stdlib"))
}
