package com.yapp.todakun.compatibility.port.outbound

/** 궁합 AI 프롬프트에 넣을 명식 한 기둥(간지). 일주 천간처럼 십성이 없으면 [stemSipseong]은 null이다. */
data class CompatibilityPillar(
    val stem: String,
    val branch: String,
    val stemSipseong: String?,
    val branchSipseong: String,
    val sibiunseong: String,
)
