package com.yapp.todakun.shared

/** 사주 명식 한 기둥(간지)의 크로스 도메인 축약 뷰. 일주 천간처럼 십성이 없는 경우 [stemSipseong]은 null이다. */
data class PillarSummary(
    val stem: String,
    val branch: String,
    val stemSipseong: String?,
    val branchSipseong: String,
    val sibiunseong: String,
)
