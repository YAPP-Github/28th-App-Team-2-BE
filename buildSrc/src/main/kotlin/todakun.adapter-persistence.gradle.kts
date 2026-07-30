// 아웃바운드 영속성 어댑터(adapter-out) 공통: JPA + Lombok(Java 엔티티) + Testcontainers 통합 테스트.
// 도메인/shared 및 도메인 고유 의존성(spring-ai, firebase 등)은 각 모듈이 직접 선언한다.
plugins {
    id("todakun.spring")
    id("todakun.lombok")
}

dependencies {
    implementation(project(":common-persistence"))
    implementation(libs.findLibrary("spring-boot-starter-data-jpa").get())

    libs.findBundle("testcontainers").get().get().forEach { testImplementation(it) }
    testImplementation(libs.findLibrary("kotest-extensions-spring").get())
    testRuntimeOnly(libs.findLibrary("postgresql").get())
}
