// @Loggable 어노테이션 처리(KSP): 클래스별 로거 확장 프로퍼티를 컴파일 타임에 자동 생성한다.
// LoggerFactory.getLogger(...)를 직접 쓰는 모듈에만 적용한다(architecture-test Konsist 규칙으로 강제).
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    "implementation"(project(":common-logging"))
    "ksp"(project(":common-logging"))
}
