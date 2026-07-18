package com.yapp.todakun.saju.adapter.manseryeok

import java.time.LocalDate

/**
 * 음력 → 양력 변환기.
 *
 * manseryeok-js v1.0.8의 `solarToLunar`(안정적인 방향)를 전 구간 역추출해 만든
 * 음력 월별 시작일/길이 테이블(리소스 `manseryeok/lunar-index.txt`, 음력 1900~2050)을 사용한다.
 * 라이브러리의 `lunarToSolar`는 다수 날짜에서 버그로 실패하지만, 이 테이블은 `solarToLunar`와
 * 전 구간(55,121일) 양방향 일치가 검증되었다(추출·검증 스크립트로 회귀 샘플도 함께 생성).
 *
 * 순수 계산(프레임워크 비의존)이라 만세력 어댑터 내부 구현으로 둔다.
 */
internal object LunarSolarConverter {
    private val EPOCH = LocalDate.of(1900, 1, 1)
    private const val RESOURCE_PATH = "/manseryeok/lunar-index.txt"

    /** 한 음력 연도의 월 구성. [leapMonth] 0이면 윤달 없음. 배열은 시간순(윤달은 기준월 바로 뒤). */
    private class LunarYear(
        val leapMonth: Int,
        val monthStartOffsets: IntArray,
        val monthLengths: IntArray,
    )

    private val table: Map<Int, LunarYear> by lazy { load() }

    private fun load(): Map<Int, LunarYear> {
        val text =
            javaClass.getResourceAsStream(RESOURCE_PATH)?.bufferedReader()?.use { it.readText() }
                ?: error("음력 인덱스 리소스를 찾을 수 없습니다: $RESOURCE_PATH")

        return text
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                val (year, leap, firstOffset, lengthsCsv) = line.split(":")
                val lengths = lengthsCsv.split(",").map(String::toInt).toIntArray()
                val starts = IntArray(lengths.size)
                var offset = firstOffset.toInt()
                for (i in lengths.indices) {
                    starts[i] = offset
                    offset += lengths[i]
                }
                year.toInt() to LunarYear(leap.toInt(), starts, lengths)
            }.toMap()
    }

    /**
     * 음력([lunarYear]/[lunarMonth]/[lunarDay], [isLeapMonth]) → 양력 [LocalDate].
     * 존재하지 않는 윤달·범위 밖 일자·지원 상한(음력 2050/11) 초과 등 유효하지 않은 음력이면 null.
     */
    fun lunarToSolar(
        lunarYear: Int,
        lunarMonth: Int,
        lunarDay: Int,
        isLeapMonth: Boolean,
    ): LocalDate? {
        val year = table[lunarYear] ?: return null
        if (isLeapMonth && lunarMonth != year.leapMonth) return null
        val slot = slotIndex(lunarMonth, isLeapMonth, year.leapMonth)
        if (slot !in year.monthStartOffsets.indices) return null
        if (lunarDay < 1 || lunarDay > year.monthLengths[slot]) return null
        return EPOCH.plusDays((year.monthStartOffsets[slot] + lunarDay - 1).toLong())
    }

    /** (월, 윤달 여부) → 그 해 음력 월들의 시간순 인덱스. 윤달은 기준월 바로 뒤에 온다. */
    private fun slotIndex(
        month: Int,
        isLeap: Boolean,
        leapMonth: Int,
    ): Int =
        when {
            isLeap -> leapMonth
            leapMonth == 0 || month <= leapMonth -> month - 1
            else -> month
        }
}
