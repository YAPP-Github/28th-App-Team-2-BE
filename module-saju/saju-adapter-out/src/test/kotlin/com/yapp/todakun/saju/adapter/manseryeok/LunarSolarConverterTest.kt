package com.yapp.todakun.saju.adapter.manseryeok

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * 음력→양력 변환 회귀 테스트.
 * 검증셋(`manseryeok/lunar-validation.tsv`)은 manseryeok-js v1.0.8의 solarToLunar에서 추출한
 * (음력 → 양력) 정답쌍(윤달 78개 포함 443개)으로, 변환기 출력이 라이브러리와 일치함을 보증한다.
 * 형식: lunarYear, lunarMonth, isLeap(0/1), lunarDay, solarYear, solarMonth, solarDay (탭 구분)
 */
class LunarSolarConverterTest : DescribeSpec({
    describe("lunarToSolar - 라이브러리 추출 검증셋 전수 대조") {
        val samples =
            LunarSolarConverterTest::class.java
                .getResourceAsStream("/manseryeok/lunar-validation.tsv")!!
                .bufferedReader()
                .useLines { lines ->
                    lines.filter { it.isNotBlank() }.map { it.split("\t").map(String::toInt) }.toList()
                }

        it("검증셋이 충분히 로드된다(윤달 포함)") {
            samples.size shouldBe 443
            samples.count { it[2] == 1 } shouldBe 78
        }

        it("모든 (음력 → 양력) 정답쌍과 일치한다") {
            samples.forEach { r ->
                LunarSolarConverter.lunarToSolar(r[0], r[1], r[3], r[2] == 1) shouldBe LocalDate.of(r[4], r[5], r[6])
            }
        }
    }

    describe("lunarToSolar - 유효하지 않은 음력은 null") {
        it("존재하지 않는 윤달") {
            // 2001년 윤달은 4월뿐 → 윤5월 없음
            LunarSolarConverter.lunarToSolar(2001, 5, 1, isLeapMonth = true).shouldBeNull()
        }
        it("해당 월 일수 초과") {
            // 음력 2001 평4월은 29일까지 → 30일 없음
            LunarSolarConverter.lunarToSolar(2001, 4, 30, isLeapMonth = false).shouldBeNull()
        }
        it("지원 범위 밖 음력 연도") {
            LunarSolarConverter.lunarToSolar(1899, 1, 1, isLeapMonth = false).shouldBeNull()
            LunarSolarConverter.lunarToSolar(2051, 1, 1, isLeapMonth = false).shouldBeNull()
        }
        it("라이브러리 데이터 상한(음력 2050/12) 초과") {
            LunarSolarConverter.lunarToSolar(2050, 12, 1, isLeapMonth = false).shouldBeNull()
        }
    }
})
