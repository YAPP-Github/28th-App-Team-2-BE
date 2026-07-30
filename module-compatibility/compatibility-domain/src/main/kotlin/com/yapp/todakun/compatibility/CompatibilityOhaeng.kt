package com.yapp.todakun.compatibility

/** 두 명식의 오행 글자 수를 합산·정규화한 한 행. [percentage]는 정수 비율(0~100, 5개 합계 정확히 100). */
data class CompatibilityOhaeng(
    val element: CompatibilityElement,
    val percentage: Int,
)
