package com.yapp.todakun.saju.port.outbound

import com.yapp.todakun.saju.EarthlyBranch
import com.yapp.todakun.saju.HeavenlyStem

/** 만세력 계산이 산출한 한 기둥의 간지(干支). 천간+지지 쌍. */
data class GanjiPillar(
    val stem: HeavenlyStem,
    val branch: EarthlyBranch,
)
