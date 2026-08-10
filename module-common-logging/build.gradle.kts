// @Loggable 어노테이션 + KSP 프로세서(LoggableSymbolProcessor). 로거 필드 선언을 어노테이션 하나로 대체하기 위한 코드 생성기.
plugins {
    id("todakun.kotlin-common")
}

dependencies {
    implementation(libs.ksp.symbol.processing.api)
}
