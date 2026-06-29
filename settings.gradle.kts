rootProject.name = "todakun"

include("bootstrap")
include("common")
include("common-web")
include("architecture-test")

// 모듈 디렉터리는 `module-*` 접두사를 쓰므로 프로젝트 경로를 매핑한다.
project(":bootstrap").projectDir = file("module-bootstrap")
project(":common").projectDir = file("module-common")
project(":common-web").projectDir = file("module-common-web")
project(":architecture-test").projectDir = file("module-architecture-test")
