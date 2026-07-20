package com.yapp.todakun.saju.port.inbound

import java.time.LocalDate
import java.util.UUID

/**
 * 상대방 사주 등록 커맨드. enum 값은 어댑터에서 문자열로 넘어와 도메인 계층에서 변환한다.
 * 음력 윤달은 미지원이라 평달로 계산한다(회원가입 정책과 동일).
 */
data class RegisterPartnerSajuCommand(
    val memberId: UUID,
    val name: String,
    val gender: String,
    val calendarType: String,
    val birthDate: LocalDate,
    val birthTime: String,
    val relationshipType: String,
)
