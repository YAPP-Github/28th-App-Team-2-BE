plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":notification:domain"))
    // 콘텐츠/동의 확장 포트의 옵셔널 주입(ObjectProvider)을 위해 spring-beans(=spring-context)가 필요.
    implementation(libs.spring.context)
}
