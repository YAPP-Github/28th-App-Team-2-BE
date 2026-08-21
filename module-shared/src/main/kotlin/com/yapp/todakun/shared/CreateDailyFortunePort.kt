package com.yapp.todakun.shared

import java.time.LocalDate
import java.util.UUID

/**
 * 오늘의 운세 생성 크로스 도메인 포트.
 * - 온보딩(회원가입): auth가 신규 회원의 당일 운세를 생성한다. 회원·사주 커밋 이후 트랜잭션 밖에서 호출되며,
 *   실패해도 가입은 되돌리지 않는다(최초 조회 시 재생성).
 * - 일일 스케줄러: daily-fortune이 전체 회원의 당일 운세를 매일 미리 생성한다.
 *
 * 구현체는 락 + saveIfAbsent로 멱등하므로 같은 (memberId, fortuneDate)로 중복 호출해도 한 건만 저장된다.
 */
interface CreateDailyFortunePort {
    fun create(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): UUID
}
