package com.yapp.todakun.saju

/**
 * 십이운성(十二運星, 포태법). 일간을 기준으로 지지의 기운 단계를 판정한다.
 * 선언 순서가 장생→목욕→…→양의 순환 순서다. 각 값의 의미는 만세력 지침 5.2절을 따른다.
 */
enum class Sibiunseong(
    val label: String,
) {
    JANGSAENG("장생"),
    MOKYOK("목욕"),
    GWANDAE("관대"),
    GEONROK("건록"),
    JEWANG("제왕"),
    SOE("쇠"),
    BYEONG("병"),
    SA("사"),
    MYO("묘"),
    JEOL("절"),
    TAE("태"),
    YANG("양"),
    ;

    companion object {
        /** 일간별 장생(長生) 지지. 양간은 순행, 음간은 역행으로 12운성을 돈다. */
        private fun jangsaeng(dayMaster: HeavenlyStem): EarthlyBranch =
            when (dayMaster) {
                HeavenlyStem.GAP -> EarthlyBranch.HAE
                HeavenlyStem.BYEONG, HeavenlyStem.MU -> EarthlyBranch.IN
                HeavenlyStem.GYEONG -> EarthlyBranch.SA
                HeavenlyStem.IM -> EarthlyBranch.SIN
                HeavenlyStem.EUL -> EarthlyBranch.O
                HeavenlyStem.JEONG, HeavenlyStem.GI -> EarthlyBranch.YU
                HeavenlyStem.SIN -> EarthlyBranch.JA
                HeavenlyStem.GYE -> EarthlyBranch.MYO
            }

        /** 일간([dayMaster]) 기준 지지([branch])의 십이운성을 판정한다. */
        fun of(
            dayMaster: HeavenlyStem,
            branch: EarthlyBranch,
        ): Sibiunseong {
            val direction = if (dayMaster.yinYang == YinYang.YANG) 1 else -1
            val start = jangsaeng(dayMaster).ordinal
            val offset = (((branch.ordinal - start) * direction) % 12 + 12) % 12
            return entries[offset]
        }
    }
}
