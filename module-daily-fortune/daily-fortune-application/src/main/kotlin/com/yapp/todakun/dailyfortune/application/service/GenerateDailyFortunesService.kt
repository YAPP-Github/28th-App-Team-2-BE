package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.port.inbound.GenerateDailyFortunesUseCase
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.GetMemberIdsPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * [GenerateDailyFortunesUseCase] 구현체.
 * 전체 회원을 순회하며 [CreateDailyFortunePort]로 오늘의 운세 생성을 요청한다.
 * 회원별 예외는 runCatching으로 격리한다. 한 명의 실패가 나머지 회원 처리를 막지 않는다.
 * [CreateDailyFortunePort] 구현체가 이미 자체 트랜잭션(@CommandService)을 갖기 때문에, 트랜잭션을 걸지 않는다.
 * 따라서 회원별 호출은 각각 독립된 트랜잭션으로 커밋/롤백된다.
 */
@Component
class GenerateDailyFortunesService(
    private val getMemberIdsPort: GetMemberIdsPort,
    private val createDailyFortunePort: CreateDailyFortunePort,
) : GenerateDailyFortunesUseCase {
    override fun generate(fortuneDate: LocalDate) {
        getMemberIdsPort.getMemberIds().forEach { memberId ->
            runCatching { createDailyFortunePort.create(memberId, fortuneDate) }
                .onFailure { log.error("일일 운세 생성 실패: memberId=$memberId, fortuneDate=$fortuneDate", it) }
        }
    }
}

private val log = LoggerFactory.getLogger(GenerateDailyFortunesService::class.java)
