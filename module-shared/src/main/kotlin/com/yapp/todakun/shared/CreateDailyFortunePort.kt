package com.yapp.todakun.shared

import java.time.LocalDate
import java.util.UUID

/**
 * 오늘의 운세 생성 크로스 도메인 포트.
 * - 온보딩(회원가입): auth가 신규 회원의 당일 운세를 생성한다. 회원가입 트랜잭션 안에서 호출되어 원자성을 보장한다.
 * - 일일 스케줄러: daily-fortune이 전체 회원의 당일 운세를 매일 미리 생성한다.
 */
interface CreateDailyFortunePort {
    fun create(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): UUID
}
