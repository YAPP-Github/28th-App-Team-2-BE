package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.CreateLuckActionPort
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 오늘의 운세 생성의 짧은 DB 트랜잭션 경계를 소유하는 협력 빈.
 * 외부 AI 호출은 이 빈 밖(오케스트레이터)에서 실행해, advisory lock·DB 커넥션을 AI 네트워크 I/O 동안 점유하지 않게 한다.
 * 각 메서드가 독립 트랜잭션이므로, [findExistingWithLock]의 락은 커밋과 함께 해제된다 → AI 생성 구간에는 락이 없다.
 * 멱등성은 [saveIfAbsent]가 락을 다시 잡고 재조회한 뒤 저장하는 것으로 보장한다(동시 요청 시 유니크 제약 충돌 방지).
 * DailyFortune 저장과 카테고리별 LuckAction 생성은 같은 트랜잭션으로 원자적으로 처리한다.
 */
@CommandService
class DailyFortuneTransactionalStore(
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val createLuckActionPort: CreateLuckActionPort,
) {
    /** 짧은 트랜잭션에서 (memberId, fortuneDate) 락을 잡고 기존 결과를 조회한다(AI 호출 전 멱등 선조회). */
    fun findExistingWithLock(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): DailyFortune? {
        dailyFortuneRepository.lock(memberId, fortuneDate)
        return dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate)
    }

    /**
     * 짧은 트랜잭션에서 락을 다시 잡고 재조회해, 이미 생성된 결과가 있으면 그 ID를 반환하고 없을 때만 저장한다(멱등 저장).
     * 새로 저장할 때는 카테고리별 LuckAction까지 같은 트랜잭션에서 생성한다.
     */
    @ExperimentalUuidApi
    fun saveIfAbsent(
        dailyFortune: DailyFortune,
        categoryFortunes: List<GeneratedCategoryFortune>,
    ): UUID {
        dailyFortuneRepository.lock(dailyFortune.memberId, dailyFortune.fortuneDate)
        dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, dailyFortune.fortuneDate)?.let { return it.id }

        val saved = dailyFortuneRepository.save(dailyFortune)

        categoryFortunes.forEach {
            createLuckActionPort.create(
                memberId = saved.memberId,
                fortuneCategory = it.fortuneCategory,
                fortuneDate = saved.fortuneDate,
                score = it.score,
                title = it.title,
                content = it.content,
            )
        }

        return saved.id
    }
}
