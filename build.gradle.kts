allprojects {
    group = "com.yapp.todakun"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// 중첩 도메인 모듈(:auth:domain, :member:domain 등)은 leaf 이름에 레이어명만 쓰므로 서로 다른 도메인이어도 프로젝트 name이 같아진다.
// group·version까지 동일하면 Gradle이 같은 모듈 좌표로 오인해 의존성 충돌 해결 과정에서 한쪽을 다른 쪽으로 치환(evict)해버린다.
// 도메인 컨테이너 하위 모듈은 group에 도메인명을 붙여 좌표를 분리한다.
subprojects {
    val segments = path.removePrefix(":").split(":")
    if (segments.size == 2) {
        group = "${rootProject.group}.${segments[0]}"
    }
}
