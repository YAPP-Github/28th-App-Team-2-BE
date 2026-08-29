package com.yapp.todakun.shared

import java.time.LocalDate

/** 오늘의 운세 생성에 필요한 회원 쪽 입력값의 크로스 도메인 스냅샷. */
data class MemberFortuneProfile(
    val name: String,
    val birthDate: LocalDate,
    val gender: String,
    val job: String,
    val relationshipStatus: String,
)
