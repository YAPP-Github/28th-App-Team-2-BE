package com.yapp.todakun.saju.adapter.manseryeok

import com.yapp.todakun.saju.EarthlyBranch

/**
 * 24절기 기반 사주월(절월) 경계 테이블.
 *
 * manseryeok-js v1.0.8 `getGapja`의 월지(절기 기준) 변경일을 연도별로 추출한 것으로,
 * 라이브러리와 전 범위(1900~2050, 55,151일) 월지가 일치한다(리소스 `manseryeok/solar-terms.txt`).
 * 라인 형식: `연도:md0,...,md11` (md=월*100+일, 소한→대설 12개 절(節)의 시작일).
 *
 * 종전의 고정 근사표(입춘=2/4 등)를 대체해 월주·년주(입춘 경계)를 절기 기준으로 정확히 계산한다.
 */
internal object SolarTermTable {
    private const val RESOURCE_PATH = "/manseryeok/solar-terms.txt"

    /** 12절(節) 순서(양력 연중): 소한→대설. 각 절이 시작하는 절월의 절기명. */
    val TERM_NAMES =
        listOf("소한", "입춘", "경칩", "청명", "입하", "망종", "소서", "입추", "백로", "한로", "입동", "대설")

    /** 각 절이 시작하는 절월의 지지(소한=丑 … 대설=子). */
    val TERM_BRANCHES =
        listOf(
            EarthlyBranch.CHUK, EarthlyBranch.IN, EarthlyBranch.MYO, EarthlyBranch.JIN,
            EarthlyBranch.SA, EarthlyBranch.O, EarthlyBranch.MI, EarthlyBranch.SIN,
            EarthlyBranch.YU, EarthlyBranch.SUL, EarthlyBranch.HAE, EarthlyBranch.JA,
        )

    /** 입춘 = 두 번째 절(년주 경계). */
    private const val IPCHUN_INDEX = 1

    /** 소한 이전(전해 대설=子월)일 때의 절 인덱스. */
    private const val DAESEOL_INDEX = 11

    private val table: Map<Int, IntArray> by lazy { load() }

    private fun load(): Map<Int, IntArray> {
        val text =
            javaClass.getResourceAsStream(RESOURCE_PATH)?.bufferedReader()?.use { it.readText() }
                ?: error("절기 리소스를 찾을 수 없습니다: $RESOURCE_PATH")

        return text
            .lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val (year, monthDays) = line.split(":")
                year.toInt() to monthDays.split(",").map(String::toInt).toIntArray()
            }
    }

    private fun boundaries(year: Int): IntArray = table[year] ?: error("절기 지원 범위 밖 연도: $year")

    /** [monthDay](=월*100+일)가 속한 절(節) 인덱스(0=소한 … 11=대설). 소한 이전이면 전해 대설(11). */
    fun termIndex(
        year: Int,
        monthDay: Int,
    ): Int {
        val mds = boundaries(year)
        var index = -1
        for (i in mds.indices) {
            if (monthDay >= mds[i]) index = i
        }
        return if (index < 0) DAESEOL_INDEX else index
    }

    /** [monthDay]가 입춘 이전인지(true면 년주는 전년도 간지). */
    fun isBeforeIpchun(
        year: Int,
        monthDay: Int,
    ): Boolean = monthDay < boundaries(year)[IPCHUN_INDEX]
}
