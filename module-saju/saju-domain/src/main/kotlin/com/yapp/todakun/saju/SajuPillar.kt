package com.yapp.todakun.saju

/**
 * 4주 각 기둥의 상세. [stemSipseong]은 천간 십성(일주 천간은 "일원"이라 null),
 * [branchSipseong]은 지지 십성, [sibiunseong]은 지지 기준 십이운성.
 */
data class SajuPillar(
    val pillarType: PillarType,
    val stem: HeavenlyStem,
    val branch: EarthlyBranch,
    val stemSipseong: Sipseong?,
    val branchSipseong: Sipseong,
    val sibiunseong: Sibiunseong,
)
