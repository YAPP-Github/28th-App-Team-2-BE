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
// StepScope: @Bean 팩토리 메서드가 실제 스코프를 부여하는 지점이라 클래스 자체엔 @StepScope를 붙여도 Spring에
// 아무 영향이 없지만(런타임 스코프는 메서드의 애노테이션만 읽음), 이 all-open 플러그인은 클래스 선언부의
// 애노테이션만 컴파일 타임에 스캔하므로 CGLIB 프록시 대상 클래스에는 별도로 클래스 레벨에도 붙여줘야 한다.
allOpen {
    annotation("com.yapp.todakun.common.annotation.CommandService")
    annotation("com.yapp.todakun.common.annotation.QueryService")
    annotation("org.springframework.batch.core.configuration.annotation.StepScope")
}
