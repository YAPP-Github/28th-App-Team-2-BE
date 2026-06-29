import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

// precompiled script plugin(.gradle.kts)은 타입세이프 `libs` 접근자를 생성하지 않으므로
// 루트 버전 카탈로그를 런타임 조회한다. 컨벤션 플러그인 전역에서 공유하는 단일 진입점.
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")