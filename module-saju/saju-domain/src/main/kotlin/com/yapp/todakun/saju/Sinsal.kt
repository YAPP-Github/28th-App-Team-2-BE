package com.yapp.todakun.saju

/**
 * 십이신살(十二神煞). 기준 지지(보통 년지)의 삼합국(三合局)을 기준으로 각 지지의 신살을 판정한다.
 * 선언 순서가 겁살→…→화개의 순환 순서이며, 각 삼합국의 겁살 위치에서 지지 순행으로 12신살이 배정된다.
 */
enum class Sinsal(
    val label: String,
) {
    GEOPSAL("겁살"),
    JAESAL("재살"),
    CHEONSAL("천살"),
    JISAL("지살"),
    NYEONSAL("년살"),
    WOLSAL("월살"),
    MANGSINSAL("망신살"),
    JANGSEONGSAL("장성살"),
    BANANSAL("반안살"),
    YEONGMASAL("역마살"),
    YUKHAESAL("육해살"),
    HWAGAESAL("화개살"),
    ;

    companion object {
        /** 기준 지지([reference])가 속한 삼합국의 겁살(첫 신살) 지지. */
        private fun geopsalBranch(reference: EarthlyBranch): EarthlyBranch =
            when (reference) {
                EarthlyBranch.SIN, EarthlyBranch.JA, EarthlyBranch.JIN -> EarthlyBranch.SA // 申子辰
                EarthlyBranch.SA, EarthlyBranch.YU, EarthlyBranch.CHUK -> EarthlyBranch.IN // 巳酉丑
                EarthlyBranch.IN, EarthlyBranch.O, EarthlyBranch.SUL -> EarthlyBranch.HAE // 寅午戌
                EarthlyBranch.HAE, EarthlyBranch.MYO, EarthlyBranch.MI -> EarthlyBranch.SIN // 亥卯未
            }

        /** 기준 지지([reference], 보통 년지) 대비 대상 지지([target])의 신살을 판정한다. */
        fun of(
            reference: EarthlyBranch,
            target: EarthlyBranch,
        ): Sinsal {
            val start = geopsalBranch(reference).ordinal
            val offset = ((target.ordinal - start) % entries.size + entries.size) % entries.size
            return entries[offset]
        }
    }
}
