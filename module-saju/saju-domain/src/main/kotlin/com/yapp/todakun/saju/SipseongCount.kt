package com.yapp.todakun.saju

/** 십성 분포 집계 한 행. 일간(일원)을 제외한 판정 대상 글자 중 [count]개, [percentage]는 비율(%). */
data class SipseongCount(
    val sipseong: Sipseong,
    val count: Int,
    val percentage: Double,
)
