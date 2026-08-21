plugins {
    id("todakun.spring")
    id("todakun.logging")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":notification:domain"))
    // 콘텐츠/동의 확장 포트의 옵셔널 주입(ObjectProvider)을 위해 spring-beans(=spring-context)가 필요.
    implementation(libs.spring.context)
    // 대상 회원별 발송 실패 로깅(버전은 Boot BOM 관리).
    implementation(libs.slf4j.api)
    // 발송 성공/실패/재시도 소진 카운터 등록(NotificationMetrics).
    implementation(libs.micrometer.core)
    // 회원별 발송/재시도(forEach)를 병렬 처리(async/awaitAll)하기 위함(이슈 #81).
    implementation(libs.kotlinx.coroutines.core)
}
