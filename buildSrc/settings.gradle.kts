// buildSrc는 별도 빌드라 루트 버전 카탈로그를 자동 상속하지 않는다.
// 동일 TOML을 import 해 컨벤션 플러그인 클래스패스 버전을 단일 소스로 공유한다.
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}