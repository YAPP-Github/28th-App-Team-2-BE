plugins {
    id("todakun.kotlin-common")
    kotlin("plugin.spring")
}

dependencies {
    // Kotlin+Spring 빈을 쓰는 모든 모듈 공통(프록시·리플렉션). 버전은 Boot BOM 관리.
    implementation(libs.findLibrary("kotlin-reflect").get())
}

// kotlin-spring 프리셋은 @CommandService/@QueryService 같은 커스텀 합성 스테레오타입까지는
// 메타 애노테이션 추적을 보장하지 않아 CGLIB 프록시 대상 클래스가 final로 남는 문제가 있어 명시 등록한다.
allOpen {
    annotation("com.yapp.todakun.common.annotation.CommandService")
    annotation("com.yapp.todakun.common.annotation.QueryService")
}
