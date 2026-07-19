package com.yapp.todakun.saju.adapter.manseryeok

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * 년/월/일주 회귀 테스트.
 * 검증셋(`manseryeok/solar-terms-validation.tsv`)은 manseryeok-js v1.0.8 `getGapja`에서 추출한
 * **모든 24절기 경계 ±1일**(정확도가 가장 중요한 날) 정답으로, 어댑터 출력이 라이브러리와 일치함을 보증한다.
 * 형식: solarYear, solarMonth, solarDay, 년주(한자), 월주(한자), 일주(한자) (탭 구분)
 */
class SolarTermPillarTest : DescribeSpec({
    val adapter = ManseryeokAdapter()

    describe("년/월/일주 - 절기 경계 검증셋 전수 대조") {
        val samples =
            SolarTermPillarTest::class.java
                .getResourceAsStream("/manseryeok/solar-terms-validation.tsv")!!
                .bufferedReader()
                .useLines { lines -> lines.filter { it.isNotBlank() }.map { it.split("\t") }.toList() }

        it("검증셋이 로드된다") {
            samples.size shouldBe 495
        }

        it("모든 절기 경계 인접일의 년·월·일주가 라이브러리와 일치한다") {
            samples.forEach { r ->
                val result =
                    adapter.calculate(
                        LocalDate.of(r[0].toInt(), r[1].toInt(), r[2].toInt()),
                        BirthTime.OSI,
                        CalendarType.SOLAR,
                        false,
                    )
                (result.year.stem.hanja + result.year.branch.hanja) shouldBe r[3]
                (result.month.stem.hanja + result.month.branch.hanja) shouldBe r[4]
                (result.day.stem.hanja + result.day.branch.hanja) shouldBe r[5]
            }
        }
    }
})
