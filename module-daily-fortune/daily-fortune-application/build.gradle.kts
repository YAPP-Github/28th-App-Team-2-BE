plugins {
    id("todakun.spring")
    id("todakun.logging")
}

dependencies {
    implementation(project(":daily-fortune:domain"))
    implementation(project(":shared"))
    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.slf4j.api)

    // Spring Batch — 오늘의 운세 생성 배치(Job/Step/Chunk). 버전은 Boot BOM 관리.
    implementation(libs.spring.boot.starter.batch)
    // Boot 4의 BatchAutoConfiguration은 기본 인메모리 JobRepository만 제공해 JDBC 기반으로 직접 오버라이드해야 한다(BatchJdbcConfig 참고).
    // 그 배치 메타데이터 스키마 초기화(DataSourceScriptDatabaseInitializer)에 필요.
    implementation(libs.spring.boot.jdbc)
}
