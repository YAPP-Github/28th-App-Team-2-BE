package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.dailyfortune.port.inbound.GenerateDailyFortunesUseCase
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.GetMemberIdsPort
import java.time.LocalDate
import java.util.UUID

// Spring Batch(GenerateDailyFortunesBatchService) 도입 전 순차 구현. Spring 빈으로 등록하지 않고,
// before/after 성능 비교 벤치마크에서 직접 생성해 비교군으로만 쓴다.
internal const val PAGE_SIZE = 100

/**
 * [GenerateDailyFortunesUseCase] 구현체.
 * 전체 회원을 keyset 페이징으로 순회하며 [CreateDailyFortunePort]로 오늘의 운세 생성을 요청한다.
 * 회원별 예외는 격리한다. 한 명의 실패가 나머지 회원 처리를 막지 않는다.
 * OutOfMemoryError 등 복구 불가능한 [Error]는 격리 대상이 아니므로 [Exception]만 명시적으로 잡아 전파시킨다.
 * [CreateDailyFortunePort] 구현체(오케스트레이터)가 저장 단계에서 회원별로 독립 트랜잭션을 커밋하기 때문에, 이 배치에는 트랜잭션을 걸지 않는다.
 * 따라서 회원별 호출은 각각 독립된 트랜잭션으로 커밋되고, AI 호출은 트랜잭션 밖에서 수행된다.
 */
@Loggable
class GenerateDailyFortunesService(
    private val getMemberIdsPort: GetMemberIdsPort,
    private val createDailyFortunePort: CreateDailyFortunePort,
) : GenerateDailyFortunesUseCase {
    override fun generate(fortuneDate: LocalDate) {
        var afterMemberId: UUID? = null

        while (true) {
            val memberIds = getMemberIdsPort.getMemberIds(afterMemberId, PAGE_SIZE)
            if (memberIds.isEmpty()) return

            memberIds.forEach { memberId ->
                try {
                    createDailyFortunePort.create(memberId, fortuneDate)
                } catch (e: Exception) {
                    log.error("일일 운세 생성 실패: memberId=$memberId, fortuneDate=$fortuneDate", e)
                }
            }

            afterMemberId = memberIds.last()
        }
    }
}
