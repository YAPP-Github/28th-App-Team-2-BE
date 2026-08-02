package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.port.inbound.GenerateDailyFortunesUseCase
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.GetMemberIdsPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

// Spring Batch(Job/Step/Chunk) 도입 전 "before" 성능 베이스라인용 순차 구현. 배치 구현 완료 후 대체된다.
internal const val PAGE_SIZE = 100

/**
 * [GenerateDailyFortunesUseCase] 구현체.
 * 전체 회원을 keyset 페이징으로 순회하며 [CreateDailyFortunePort]로 오늘의 운세 생성을 요청한다.
 * 회원별 예외는 격리한다. 한 명의 실패가 나머지 회원 처리를 막지 않는다.
 * OutOfMemoryError 등 복구 불가능한 [Error]는 격리 대상이 아니므로 [Exception]만 명시적으로 잡아 전파시킨다.
 * [CreateDailyFortunePort] 구현체가 이미 자체 트랜잭션(@CommandService)을 갖기 때문에, 트랜잭션을 걸지 않는다.
 * 따라서 회원별 호출은 각각 독립된 트랜잭션으로 커밋/롤백된다.
 */
@Component
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

private val log = LoggerFactory.getLogger(GenerateDailyFortunesService::class.java)
