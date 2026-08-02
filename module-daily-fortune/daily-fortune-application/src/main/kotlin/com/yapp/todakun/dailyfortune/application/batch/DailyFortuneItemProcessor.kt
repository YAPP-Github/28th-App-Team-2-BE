package com.yapp.todakun.dailyfortune.application.batch

import com.yapp.todakun.shared.CreateDailyFortunePort
import org.springframework.batch.infrastructure.item.ItemProcessor
import java.time.LocalDate
import java.util.UUID

/**
 * 회원 1명의 오늘의 운세 생성을 처리한다.
 * [CreateDailyFortunePort.create]가 AI 호출부터 저장까지 자체 트랜잭션(@CommandService)으로 커밋하므로,
 * 이 Processor는 별도 상태 없이 결과를 위임만 한다.
 */
class DailyFortuneItemProcessor(
    private val createDailyFortunePort: CreateDailyFortunePort,
    private val fortuneDate: LocalDate,
) : ItemProcessor<UUID, UUID> {
    override fun process(item: UUID): UUID = createDailyFortunePort.create(item, fortuneDate)
}
