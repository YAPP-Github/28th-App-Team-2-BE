package com.yapp.todakun.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

/**
 * 헥사고날 아키텍처 규칙(Konsist) 검증.
 * 새 도메인을 추가하면 대부분의 규칙이 자동으로 새 클래스에 적용된다.
 */
class ArchitectureTest {
    private val scope = Konsist.scopeFromProject()

    /** 순수 도메인 클래스(레이어/공통 모듈/부트스트랩 제외). */
    private val domainClasses
        get() =
            scope.classes().filter { clazz ->
                !clazz.resideInPackage("..application..") &&
                    !clazz.resideInPackage("..adapter..") &&
                    !clazz.resideInPackage("..shared..") &&
                    !clazz.resideInPackage("..common..") &&
                    !clazz.resideInPackage("..architecture..") &&
                    !clazz.resideInPackage("..web..") &&
                    !clazz.resideInPackage("..config..") &&
                    clazz.annotations.none { it.name == "SpringBootApplication" }
            }

    @Test
    fun `도메인 클래스는 Spring 어노테이션을 사용하지 않는다`() {
        domainClasses.forEach { clazz ->
            clazz.annotations.forEach { annotation ->
                check(!(annotation.fullyQualifiedName ?: "").startsWith("org.springframework")) {
                    "도메인 클래스 ${clazz.name}에 Spring 어노테이션(${annotation.name}) 사용 불가"
                }
            }
        }
    }

    @Test
    fun `도메인 클래스는 JPA 어노테이션을 사용하지 않는다`() {
        domainClasses.forEach { clazz ->
            clazz.annotations.forEach { annotation ->
                check(!(annotation.fullyQualifiedName ?: "").startsWith("jakarta.persistence")) {
                    "도메인 클래스 ${clazz.name}에 JPA 어노테이션(${annotation.name}) 사용 불가"
                }
            }
        }
    }

    @Test
    fun `CQRS 서비스는 application 패키지에만 위치한다`() {
        scope
            .classes()
            .filter { clazz -> clazz.annotations.any { it.name == "CommandService" || it.name == "QueryService" } }
            .forEach { clazz ->
                check(clazz.resideInPackage("..application..")) {
                    "${clazz.name}는 .application 패키지에 위치해야 한다"
                }
            }
    }

    @Test
    fun `Controller는 Api 인터페이스를 구현한다`() {
        scope
            .classes()
            .filter { it.name.endsWith("Controller") }
            .forEach { clazz ->
                check(clazz.hasParentInterface { it.name.endsWith("Api") }) {
                    "${clazz.name}는 *Api 인터페이스를 구현해야 한다"
                }
            }
    }

    @Test
    fun `UseCase 인터페이스는 domain의 port_inbound 패키지에만 위치한다`() {
        scope
            .interfaces()
            .filter { it.name.endsWith("UseCase") }
            .forEach { iface ->
                check(iface.resideInPackage("..port.inbound..")) {
                    "${iface.name}는 port.inbound 패키지에 위치해야 한다"
                }
            }
    }

    @Test
    fun `아웃바운드 Port 인터페이스는 domain의 port_outbound 패키지에만 위치한다`() {
        scope
            .interfaces()
            .filter { it.name.endsWith("Port") && !it.resideInPackage("..shared..") }
            .forEach { iface ->
                check(iface.resideInPackage("..port.outbound..")) {
                    "${iface.name}는 port.outbound 패키지에 위치해야 한다"
                }
            }
    }

    @Test
    fun `JpaEntity는 adapter 패키지에만 위치한다`() {
        scope
            .classes()
            .filter { it.name.endsWith("JpaEntity") }
            .forEach { clazz ->
                check(clazz.resideInPackage("..adapter..")) {
                    "${clazz.name}는 .adapter 패키지에 위치해야 한다"
                }
            }
    }

    @Test
    fun `Adapter는 adapter 패키지에만 위치한다`() {
        scope
            .classes()
            .filter { it.name.endsWith("Adapter") }
            .forEach { clazz ->
                check(clazz.resideInPackage("..adapter..")) {
                    "${clazz.name}는 .adapter 패키지에 위치해야 한다"
                }
            }
    }

    @Test
    fun `Api 인터페이스는 adapter 패키지에만 위치한다`() {
        scope
            .interfaces()
            .filter { it.name.endsWith("Api") }
            .forEach { iface ->
                check(iface.resideInPackage("..adapter..")) {
                    "${iface.name}는 .adapter 패키지에 위치해야 한다"
                }
            }
    }

    @Test
    fun `Request DTO는 adapter 패키지에만 위치한다`() {
        scope
            .classes()
            .filter { it.name.endsWith("Request") }
            .forEach { clazz ->
                check(clazz.resideInPackage("..adapter..")) {
                    "${clazz.name}는 .adapter 패키지에 위치해야 한다"
                }
            }
    }

    @Test
    fun `Response DTO는 adapter 패키지에만 위치한다`() {
        scope
            .classes()
            .filter { it.name.endsWith("Response") && it.name != "CommonResponse" }
            .forEach { clazz ->
                check(clazz.resideInPackage("..adapter..")) {
                    "${clazz.name}는 .adapter 패키지에 위치해야 한다"
                }
            }
    }

    @Test
    fun `application 클래스는 adapter 패키지를 참조하지 않는다`() {
        scope
            .classes()
            .filter { it.resideInPackage("..application..") }
            .forEach { clazz ->
                check(clazz.containingFile.imports.none { import -> import.name.contains(".adapter.") }) {
                    "${clazz.name}(application)는 .adapter 패키지를 임포트할 수 없다"
                }
            }
    }

    @Test
    fun `레이어 의존성 방향을 지킨다`() {
        // application/adapter 레이어 중 하나라도 비어 있으면 Konsist precondition이 실패하므로, 두 레이어에 모두 파일이 존재할 때만 검증한다.
        // 아직 application 계층 파일이 없는 도메인이 추가되면 자동으로 건너뛰고, 채워지면 자동으로 활성화된다.
        val hasApplicationClasses = scope.classes().any { it.resideInPackage("..application..") }
        val hasAdapterClasses = scope.classes().any { it.resideInPackage("..adapter..") }
        if (!hasApplicationClasses || !hasAdapterClasses) return

        Konsist
            .scopeFromProject()
            .assertArchitecture {
                val domain = Layer("Domain", "com.yapp.todakun..")
                val application = Layer("Application", "com.yapp.todakun..application..")
                val adapter = Layer("Adapter", "com.yapp.todakun..adapter..")

                application.dependsOn(domain)
                adapter.dependsOn(application, domain)
            }
    }

    @Test
    fun `Firebase 타입은 adapter 패키지에서만 임포트한다`() {
        scope
            .classes()
            .filter { clazz -> clazz.containingFile.imports.any { it.name.startsWith("com.google.firebase") } }
            .forEach { clazz ->
                check(clazz.resideInPackage("..adapter..")) {
                    "${clazz.name}: com.google.firebase 임포트는 .adapter 패키지에서만 허용된다"
                }
            }
    }

    @Test
    fun `Fcm 어댑터는 adapter_fcm 패키지에만 위치한다`() {
        scope
            .classes()
            .filter { it.name.startsWith("Fcm") && it.name.endsWith("Adapter") }
            .forEach { clazz ->
                check(clazz.resideInPackage("..adapter.fcm..")) {
                    "${clazz.name}는 .adapter.fcm 패키지에 위치해야 한다"
                }
            }
    }
}
