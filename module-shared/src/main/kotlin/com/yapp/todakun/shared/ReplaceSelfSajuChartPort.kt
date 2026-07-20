package com.yapp.todakun.shared

import java.time.LocalDate
import java.util.UUID

/**
 * 회원 본인(SELF) 사주 명식을 재계산해 교체하는 크로스 도메인 포트.
 * 내 정보 수정으로 생년 정보가 바뀌면 member가 이 포트로 SELF 명식 재계산을 위임한다(saju-application 구현).
 * enum 값은 도메인 경계를 넘지 않도록 문자열로 전달한다(예: [gender]="FEMALE", [birthTime]="MISI").
 */
interface ReplaceSelfSajuChartPort {
    fun replace(
        memberId: UUID,
        name: String,
        gender: String,
        calendarType: String,
        birthDate: LocalDate,
        birthTime: String,
        isLeapMonth: Boolean,
    )
}
