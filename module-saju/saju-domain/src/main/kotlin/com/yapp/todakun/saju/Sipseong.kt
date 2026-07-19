package com.yapp.todakun.saju

import com.yapp.todakun.saju.exception.SajuCalculationException

/**
 * 십성(十星). 일간(日干)을 기준으로 대상 글자의 오행·음양 관계로 판정한다.
 * 각 값의 의미는 만세력 지침 5.2절을 따른다.
 */
enum class Sipseong(
    val label: String,
) {
    BIGYEON("비견"), // 일간과 오행·음양이 모두 같음
    GEOPJAE("겁재"), // 일간과 오행은 같으나 음양이 다름
    SIKSIN("식신"), // 일간이 生하고 음양이 같음
    SANGGWAN("상관"), // 일간이 生하고 음양이 다름
    PYEONJAE("편재"), // 일간이 剋하고 음양이 같음
    JEONGJAE("정재"), // 일간이 剋하고 음양이 다름
    PYEONGWAN("편관"), // 일간을 剋하고 음양이 같음
    JEONGGWAN("정관"), // 일간을 剋하고 음양이 다름
    PYEONIN("편인"), // 일간을 生하고 음양이 같음
    JEONGIN("정인"), // 일간을 生하고 음양이 다름
    ;

    companion object {
        /**
         * 일간([dayMaster]) 대비 대상 오행([targetElement])·음양([targetYinYang])의 십성을 판정한다.
         * 다섯 오행 관계(동일·내가生·내가剋·나를剋·나를生)는 상호 배타·전수(exhaustive)라 항상 하나만 매칭된다.
         */
        fun of(
            dayMaster: HeavenlyStem,
            targetElement: Element,
            targetYinYang: YinYang,
        ): Sipseong {
            val dm = dayMaster.element
            val sameYinYang = dayMaster.yinYang == targetYinYang
            return when {
                targetElement == dm -> if (sameYinYang) BIGYEON else GEOPJAE
                dm.generates() == targetElement -> if (sameYinYang) SIKSIN else SANGGWAN
                dm.controls() == targetElement -> if (sameYinYang) PYEONJAE else JEONGJAE
                targetElement.controls() == dm -> if (sameYinYang) PYEONGWAN else JEONGGWAN
                targetElement.generates() == dm -> if (sameYinYang) PYEONIN else JEONGIN
                else -> throw SajuCalculationException()
            }
        }
    }
}
