plugins {
    id("todakun.spring")
}

dependencies {
    implementation(project(":luck:domain"))
    implementation(libs.spring.context)
    testImplementation(project(":shared")) // 테스트 픽스처에서만 FortuneCategory 참조
}
