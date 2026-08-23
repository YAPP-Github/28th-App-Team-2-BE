package com.yapp.todakun.dailyfortune.port.outbound

import java.time.LocalDate
import java.util.UUID

/**
 * (memberId, fortuneDate) 조합에 대해 AI 생성이 동시에 중복 실행되는 걸 막는 아웃바운드 포트다.
 * DB 트랜잭션과 무관하게(AI 호출 구간 동안 DB 커넥션·락을 점유하지 않도록) 별도로 관리되는 락이다.
 * 락에는 TTL이 있어(구현체가 관리), TTL이 실제 생성 시간보다 짧아지는 설정 오류가 생기면 락이 만료된 뒤
 * 다른 호출자가 새로 선점할 수 있다. 이때 원래 호출자가 뒤늦게 [release]를 호출해 그 새 선점을 실수로
 * 지우지 않도록, [tryAcquire]가 돌려준 토큰을 [release]에 그대로 전달해 소유권을 확인한다.
 */
interface DailyFortuneGenerationLockPort {
    /** 생성을 시작해도 되면 락 소유권을 증명하는 토큰을 반환하고, 이미 다른 호출자가 생성 중이면 null. */
    fun tryAcquire(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): String?

    /** [tryAcquire]가 돌려준 토큰으로만 해제한다(성공/실패 무관하게 호출) — 토큰이 다르면 그 락을 건드리지 않는다. */
    fun release(
        memberId: UUID,
        fortuneDate: LocalDate,
        token: String,
    )
}
