rootProject.name = "todakun"

include("bootstrap")
include("common")
include("common-web")
include("shared")
include("architecture-test")

// auth 도메인 (:auth 하위에 nested된 4개 모듈, leaf는 레이어명만)
include("auth:domain")
include("auth:application")
include("auth:adapter-in")
include("auth:adapter-out")

// 모듈 디렉터리는 `module-*` 접두사를 쓰므로 프로젝트 경로를 매핑한다.
project(":bootstrap").projectDir = file("module-bootstrap")
project(":common").projectDir = file("module-common")
project(":common-web").projectDir = file("module-common-web")
project(":shared").projectDir = file("module-shared")
project(":architecture-test").projectDir = file("module-architecture-test")

// 도메인 컨테이너(:auth)는 소스가 없는 그룹 프로젝트이며, 하위 레이어 모듈을 nested로 둔다.
// (Gradle 경로 leaf는 레이어명만 쓰고, 디렉터리는 `{domain}-{layer}` 유지 → projectDir로 매핑)
project(":auth").projectDir = file("module-auth")
project(":auth:domain").projectDir = file("module-auth/auth-domain")
project(":auth:application").projectDir = file("module-auth/auth-application")
project(":auth:adapter-in").projectDir = file("module-auth/auth-adapter-in")
project(":auth:adapter-out").projectDir = file("module-auth/auth-adapter-out")
