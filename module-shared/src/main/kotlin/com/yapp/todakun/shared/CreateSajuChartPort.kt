package com.yapp.todakun.shared

import java.time.LocalDate
import java.util.UUID

/**
 * 사주 명식 계산·저장 크로스 도메인 포트. 만세력은 REST로 노출되지 않고 이 포트로만 호출된다.
 * - 회원가입: auth가 회원 본인 사주를 생성([isSelf]=true, [userId]=회원 ID)한다.
 * - 궁합: 상대방 사주를 생성([isSelf]=false)한다(추후).
 *
 * enum 값은 도메인 경계를 넘지 않도록 문자열로 전달한다(예: [gender]="FEMALE", [birthTime]="MISI").
 * 반환값은 생성된 사주 명식 ID.
 */
interface CreateSajuChartPort {
    fun create(
        userId: UUID?,
        isSelf: Boolean,
        name: String?,
        gender: String,
        calendarType: String,
        birthDate: LocalDate,
        birthTime: String,
        isLeapMonth: Boolean,
    ): UUID
}
