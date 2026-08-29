package com.yapp.todakun.chat.port.outbound

import java.time.LocalDate

/** 토닥이 AI 프롬프트에 넣을 회원 프로필. member 도메인 타입을 직접 참조하지 않도록 원시 타입으로만 구성한다. */
data class ChatProfileContext(
    val name: String,
    val birthDate: LocalDate,
    val gender: String,
    val job: String,
    val relationshipStatus: String,
)
