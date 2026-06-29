// 모든 모듈 공통 설정: Kotlin/JVM, ktlint, JDK 25 툴체인, Spring BOM, 테스트.
plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot BOM은 카탈로그를 단일 소스로 사용(좌표·버전 하드코딩 금지).
    implementation(platform(libs.findLibrary("spring-boot-dependencies").get()))

    // 모든 모듈 공통 테스트: JUnit 플랫폼 + Kotest + MockK.
    libs.findBundle("kotest").get().get().forEach { testImplementation(it) }
    testImplementation(libs.findLibrary("kotlin-test-junit5").get())
    testImplementation(libs.findLibrary("mockk").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // 모듈별 테스트를 사용 가능한 코어의 절반으로 포크 병렬 실행(최소 1).
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
