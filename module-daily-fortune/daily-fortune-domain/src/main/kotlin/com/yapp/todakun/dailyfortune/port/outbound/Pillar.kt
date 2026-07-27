package com.yapp.todakun.dailyfortune.port.outbound

/**
 * 사주 명식 기둥(년/월/일/시주) 하나를 나타낸다.
 * 일주 천간은 일원이라 [stemSipseong]이 null이다.
 */
data class Pillar(
    val stem: String,
    val branch: String,
    val stemSipseong: String?,
    val branchSipseong: String,
    val sibiunseong: String,
)
