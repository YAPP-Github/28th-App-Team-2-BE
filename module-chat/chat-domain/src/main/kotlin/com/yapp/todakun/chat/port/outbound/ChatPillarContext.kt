package com.yapp.todakun.chat.port.outbound

/** 토닥이 AI 프롬프트에 넣을 명식 한 기둥(간지). 일주 천간처럼 십성이 없으면 [stemSipseong]은 null이다. */
data class ChatPillarContext(
    val stem: String,
    val branch: String,
    val stemSipseong: String?,
    val branchSipseong: String,
    val sibiunseong: String,
)
