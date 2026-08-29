package com.yapp.todakun.saju.port.inbound

import java.time.LocalDate
import java.util.UUID

/** 상대방 사주 수정 커맨드. 생년 정보가 바뀌면 명식을 재계산한다. [linkId]는 대상 소유권 링크. */
data class UpdatePartnerSajuCommand(
    val memberId: UUID,
    val linkId: UUID,
    val name: String,
    val gender: String,
    val calendarType: String,
    val birthDate: LocalDate,
    val birthTime: String,
    val relationshipType: String,
)
