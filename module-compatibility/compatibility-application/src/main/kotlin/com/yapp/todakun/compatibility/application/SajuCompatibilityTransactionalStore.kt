package com.yapp.todakun.compatibility.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.compatibility.SajuCompatibility
import com.yapp.todakun.compatibility.port.outbound.SajuCompatibilityRepository
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 궁합 생성의 짧은 DB 트랜잭션 경계를 소유하는 협력 빈.
 * 외부 AI 호출은 이 빈 밖(오케스트레이터)에서 실행해, advisory lock·DB 커넥션을 AI 네트워크 I/O 동안 점유하지 않게 한다.
 * 각 메서드가 독립 트랜잭션이므로, [findExistingWithLock]의 락은 커밋과 함께 해제된다 → AI 생성 구간에는 락이 없다.
 * 멱등성은 [saveIfAbsent]가 락을 다시 잡고 재조회한 뒤 저장하는 것으로 보장한다(동시 요청 시 유니크 제약 충돌 방지).
 */
@CommandService
class SajuCompatibilityTransactionalStore(
    private val sajuCompatibilityRepository: SajuCompatibilityRepository,
) {
    /** 짧은 트랜잭션에서 (myChartId, partnerChartId) 락을 잡고 기존 결과를 조회한다(AI 호출 전 멱등 선조회). */
    fun findExistingWithLock(
        memberId: UUID,
        myChartId: UUID,
        partnerChartId: UUID,
    ): SajuCompatibility? {
        sajuCompatibilityRepository.lock(myChartId, partnerChartId)
        return sajuCompatibilityRepository.findByMemberIdAndCharts(memberId, myChartId, partnerChartId)
    }

    /** 짧은 트랜잭션에서 락을 다시 잡고 재조회해, 이미 생성된 결과가 있으면 그대로 반환하고 없을 때만 저장한다(멱등 저장). */
    @ExperimentalUuidApi
    fun saveIfAbsent(sajuCompatibility: SajuCompatibility): SajuCompatibility {
        sajuCompatibilityRepository.lock(sajuCompatibility.myChartId, sajuCompatibility.partnerChartId)
        return sajuCompatibilityRepository.findByMemberIdAndCharts(
            sajuCompatibility.memberId,
            sajuCompatibility.myChartId,
            sajuCompatibility.partnerChartId,
        ) ?: sajuCompatibilityRepository.save(sajuCompatibility)
    }
}
