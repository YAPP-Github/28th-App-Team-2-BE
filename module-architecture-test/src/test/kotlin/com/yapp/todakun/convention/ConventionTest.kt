package com.yapp.todakun.convention

import io.kotest.core.spec.style.DescribeSpec
import java.io.File

/**
 * 파일 포맷 컨벤션 검증. ktlint는 .kt만 검사하므로 .java/.gradle.kts/.yaml 등
 * git이 추적하는 전체 텍스트 파일을 대상으로 별도 검증한다.
 */
class ConventionTest : DescribeSpec({
    describe("추적 중인 모든 파일") {
        it("개행 문자로 끝난다") {
            val violations =
                trackedTextFiles().filter { file ->
                    val bytes = file.readBytes()
                    bytes.isNotEmpty() && bytes.last() != '\n'.code.toByte()
                }

            check(violations.isEmpty()) {
                "다음 파일이 개행 문자 없이 끝납니다(EOF 누락):\n" +
                    violations.joinToString("\n") { it.relativeTo(projectRoot).path }
            }
        }
    }

    describe("도메인 모듈") {
        it("각자 CLAUDE.md를 가진다") {
            val missing = domainModuleDirs().filterNot { File(it, "CLAUDE.md").isFile }

            check(missing.isEmpty()) {
                "다음 도메인 모듈에 CLAUDE.md가 없습니다. " +
                    "도메인 맥락은 루트가 아니라 각 모듈이 갖는다(작성 규칙: .claude/examples/domain-claude-md.md):\n" +
                    missing.joinToString("\n") { it.name }
            }
        }
    }
})

private val projectRoot: File
    get() {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (!File(dir, ".git").exists()) {
            dir = dir.parentFile ?: error("git 루트를 찾을 수 없습니다")
        }
        return dir
    }

private val binaryExtensions =
    setOf("jar", "png", "jpg", "jpeg", "gif", "ico", "svg", "woff", "woff2", "ttf", "class", "keystore", "p12")

/**
 * 도메인 모듈 디렉터리. `module-{domain}/` 중 중첩 레이어 모듈(`{domain}-domain/`)을 가진 것만 도메인으로 본다
 * — `module-common` 같은 최상위 모듈은 제외된다.
 */
private fun domainModuleDirs(): List<File> =
    projectRoot
        .listFiles { file -> file.isDirectory && file.name.startsWith("module-") }
        .orEmpty()
        .filter { dir -> File(dir, "${dir.name.removePrefix("module-")}-domain").isDirectory }
        .sortedBy { it.name }

private fun trackedTextFiles(): List<File> {
    val process =
        ProcessBuilder("git", "ls-files")
            .directory(projectRoot)
            .redirectErrorStream(true)
            .start()
    val paths = process.inputStream.bufferedReader().use { it.readLines() }
    process.waitFor()

    return paths
        .map { File(projectRoot, it) }
        .filter { it.isFile }
        .filter { it.extension.lowercase() !in binaryExtensions }
}
