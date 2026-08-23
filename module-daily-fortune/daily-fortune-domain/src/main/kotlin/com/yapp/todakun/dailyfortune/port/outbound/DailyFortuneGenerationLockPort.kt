package com.yapp.todakun.dailyfortune.port.outbound

import java.time.LocalDate
import java.util.UUID

/**
 * (memberId, fortuneDate) 조합에 대해 AI 생성이 동시에 중복 실행되는 걸 막는 아웃바운드 포트다.
 * DB 트랜잭션과 무관하게(AI 호출 구간 동안 DB 커넥션·락을 점유하지 않도록) 별도로 관리되는 락이다.
 */
interface DailyFortuneGenerationLockPort {
    /** 생성을 시작해도 되면 true, 이미 다른 호출자가 생성 중이면 false. */
    fun tryAcquire(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): Boolean

    /** 생성이 끝났음(성공/실패 무관)을 알려 락을 해제한다. */
    fun release(
        memberId: UUID,
        fortuneDate: LocalDate,
    )
}
