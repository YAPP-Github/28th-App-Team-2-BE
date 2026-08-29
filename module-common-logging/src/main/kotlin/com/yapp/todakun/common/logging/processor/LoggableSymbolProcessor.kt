package com.yapp.todakun.common.logging.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.validate
import com.yapp.todakun.common.logging.Loggable

private val LOGGABLE_ANNOTATION = requireNotNull(Loggable::class.qualifiedName)

/**
 * `@Loggable`이 붙은 클래스마다 같은 패키지에 `<ClassName>Logger.kt`를 생성한다.
 * `<ClassName>.log` 확장 프로퍼티를 선언해, 클래스 내부 메서드에서는 암시적 리시버로 `log.xxx(...)`를 그대로 쓸 수 있게 한다.
 * SLF4J의 `LoggerFactory.getLogger(...)`는 이름별로 내부 캐시를 갖고 있어 매 호출이 새 인스턴스를 만들지 않으므로, 생성 코드에서 별도로 필드에 캐싱하지 않고 getter에서 바로 호출한다.
 * 생성 코드는 패키지 최상위에 `<ClassName>.log`를 선언하는 방식이라 중첩 클래스·제네릭 클래스는 지원하지 않는다(각각 이유는 아래 검증 참고).
 */
class LoggableSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getSymbolsWithAnnotation(LOGGABLE_ANNOTATION).partition { it.validate() }

        valid
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.origin == Origin.KOTLIN }
            .forEach(::generateLoggerExtension)

        return invalid
    }

    private fun generateLoggerExtension(classDeclaration: KSClassDeclaration) {
        // 중첩 클래스는 단순 이름이 패키지 최상위 심볼이 아니라 `val <ClassName>.log`가 가리키는 대상이 없어 컴파일이 깨진다.
        if (classDeclaration.parentDeclaration != null) {
            return reportUnsupported(classDeclaration, "중첩 클래스")
        }
        // 제네릭 클래스는 원시 타입(raw type)에 대한 확장 프로퍼티를 만들 수 없어 타입 인자 누락으로 컴파일이 깨진다.
        if (classDeclaration.typeParameters.isNotEmpty()) {
            return reportUnsupported(classDeclaration, "제네릭 클래스")
        }
        val containingFile = classDeclaration.containingFile ?: return reportUnsupported(classDeclaration, "소스 파일을 찾을 수 없는 클래스")

        writeLoggerFile(
            containingFile = containingFile,
            packageName = classDeclaration.packageName.asString(),
            className = classDeclaration.simpleName.asString(),
        )
    }

    private fun reportUnsupported(
        classDeclaration: KSClassDeclaration,
        reason: String,
    ) {
        logger.error("@Loggable은 ${reason}에는 사용할 수 없습니다: ${classDeclaration.qualifiedName?.asString()}", classDeclaration)
    }

    private fun writeLoggerFile(
        containingFile: KSFile,
        packageName: String,
        className: String,
    ) {
        codeGenerator
            .createNewFile(
                dependencies = Dependencies(aggregating = false, containingFile),
                packageName = packageName,
                fileName = "${className}Logger",
            ).bufferedWriter()
            .use { writer ->
                writer.write(
                    """
                    package $packageName

                    import org.slf4j.LoggerFactory

                    internal val $className.log
                        get() = LoggerFactory.getLogger($className::class.java)

                    """.trimIndent(),
                )
            }
    }
}
