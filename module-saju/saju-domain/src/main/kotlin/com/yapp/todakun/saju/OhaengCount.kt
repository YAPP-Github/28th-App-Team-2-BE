package com.yapp.todakun.saju

/** 오행 분포 집계 한 행. [count]는 8글자(시간 모름 시 6글자) 중 개수, [percentage]는 비율(%). */
data class OhaengCount(
    val element: Element,
    val count: Int,
    val percentage: Double,
)
