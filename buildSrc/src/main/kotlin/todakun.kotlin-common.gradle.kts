// 모든 모듈 공통 설정: Kotlin/JVM, ktlint, JDK 25 툴체인, Spring BOM, 테스트.
plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

// nested 도메인 모듈은 Gradle 프로젝트 경로 leaf가 레이어명만이라 이름이 겹칠 수 있어,
// bootJar에서 BOOT-INF/lib jar 파일명이 충돌하지 않도록 전체 경로 기반으로 고유화한다.
base {
    archivesName.set(path.trimStart(':').replace(':', '-'))
}

dependencies {
    // :common은 모든 모듈이 공통으로 쓰는 전역 모듈 → 여기서 일괄 주입(자기 자신은 제외).
    if (path != ":common") {
        implementation(project(":common"))
    }

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
    // CI(예: GitHub Actions)는 코어 수가 적은데 org.gradle.parallel로 여러 모듈의 테스트 태스크까지
    // 동시에 돌아, 모듈 내 포크 병렬까지 곱해지면 같은 모듈의 TestContainer(Postgres/Redis)를 공유하는
    // 여러 통합 테스트 JVM이 동시에 뜨며 자원 경합(플레이크)이 커진다. CI에서는 모듈 내 포크를 1로 고정해
    // 그 경합만 줄인다(모듈 간 병렬 실행 자체는 유지).
    val isCi = System.getenv("CI") != null
    maxParallelForks = if (isCi) 1 else (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
