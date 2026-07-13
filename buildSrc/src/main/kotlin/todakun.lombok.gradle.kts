// Java JPA 엔티티(common-persistence, *-adapter-out)의 getter/생성자 보일러플레이트를 Lombok으로 생성.
// kotlin("plugin.lombok")을 같이 적용해, 같은 모듈에 섞인 Kotlin 코드가 Lombok 생성 멤버를 인식하도록 한다
// (미적용 시 같은 모듈 내 Kotlin -> Lombok getter 직접 참조가 unresolved reference로 실패할 수 있음).
plugins {
    kotlin("plugin.lombok")
}

dependencies {
    // 이 스크립트는 kotlin("plugin.lombok")만 적용하므로 compileOnly/annotationProcessor 타입세이프 접근자가 없다
    // (java 플러그인은 todakun.kotlin-common 등 함께 적용되는 다른 컨벤션 플러그인이 붙여준다) → 문자열 설정명으로 추가.
    "compileOnly"(libs.findLibrary("lombok").get())
    "annotationProcessor"(libs.findLibrary("lombok").get())
}