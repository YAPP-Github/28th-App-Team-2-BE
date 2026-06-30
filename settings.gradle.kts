rootProject.name = "todakun"

include("bootstrap")
include("common")
include("common-web")
include("architecture-test")

// auth 도메인 (4개 모듈)
include("auth-domain")
include("auth-application")
include("auth-adapter-in")
include("auth-adapter-out")

// 모듈 디렉터리는 `module-*` 접두사를 쓰므로 프로젝트 경로를 매핑한다.
project(":bootstrap").projectDir = file("module-bootstrap")
project(":common").projectDir = file("module-common")
project(":common-web").projectDir = file("module-common-web")
project(":architecture-test").projectDir = file("module-architecture-test")
project(":auth-domain").projectDir = file("module-auth/auth-domain")
project(":auth-application").projectDir = file("module-auth/auth-application")
project(":auth-adapter-in").projectDir = file("module-auth/auth-adapter-in")
project(":auth-adapter-out").projectDir = file("module-auth/auth-adapter-out")
