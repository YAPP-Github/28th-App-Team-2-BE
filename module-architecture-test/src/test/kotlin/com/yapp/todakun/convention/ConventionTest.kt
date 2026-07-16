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
