package com.yapp.todakun.compatibility

import com.yapp.todakun.compatibility.exception.CompatibilityOhaengEmptyException

/**
 * 두 명식의 오행 글자 수를 합산해 100 기준 정수 비율로 정규화한다(5개 오행 합계가 정확히 100).
 * 내림 후 남는 몫은 큰 나머지 방법(largest remainder)으로 소수부가 큰 오행부터 1씩 배분해 반올림 오차를 보정한다.
 * 입력 맵의 key는 오행 코드([CompatibilityElement.name], WOOD/FIRE/EARTH/METAL/WATER)이며, 없는 오행은 0으로 본다.
 */
object CompatibilityOhaengCalculator {
    private const val TOTAL_PERCENTAGE = 100

    fun combine(
        myOhaeng: Map<String, Int>,
        partnerOhaeng: Map<String, Int>,
    ): List<CompatibilityOhaeng> {
        val counts =
            CompatibilityElement.entries.associateWith { element ->
                (myOhaeng[element.name] ?: 0) + (partnerOhaeng[element.name] ?: 0)
            }
        val total = counts.values.sum()
        if (total == 0) {
            throw CompatibilityOhaengEmptyException()
        }

        return normalize(counts, total)
    }

    private fun normalize(
        counts: Map<CompatibilityElement, Int>,
        total: Int,
    ): List<CompatibilityOhaeng> {
        val exact = counts.mapValues { it.value * TOTAL_PERCENTAGE.toDouble() / total }
        val floors = exact.mapValues { it.value.toInt() }
        val remainder = TOTAL_PERCENTAGE - floors.values.sum()
        val bonusElements =
            exact.entries
                .sortedByDescending { it.value - floors.getValue(it.key) }
                .take(remainder)
                .map { it.key }
                .toSet()

        return CompatibilityElement.entries.map { element ->
            CompatibilityOhaeng(
                element = element,
                percentage = floors.getValue(element) + if (element in bonusElements) 1 else 0,
            )
        }
    }
}
