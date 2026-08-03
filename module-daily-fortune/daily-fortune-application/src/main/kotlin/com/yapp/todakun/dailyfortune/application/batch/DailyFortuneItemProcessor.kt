package com.yapp.todakun.dailyfortune.application.batch

import com.yapp.todakun.shared.CreateDailyFortunePort
import org.slf4j.LoggerFactory
import org.springframework.batch.infrastructure.item.ItemProcessor
import java.time.LocalDate
import java.util.UUID

/**
 * 회원 1명의 오늘의 운세 생성을 처리한다.
 * [CreateDailyFortunePort.create]가 AI 호출부터 저장까지 자체 트랜잭션(@CommandService)으로 커밋하므로,
 * 이 Processor는 별도 상태 없이 결과를 위임만 한다.
 * 회원별 예외는 여기서 격리한다 — null을 반환하면 Spring Batch가 해당 아이템만 필터링하고 다음 회원으로 이어간다.
 * OutOfMemoryError 등 복구 불가능한 [Error]는 격리 대상이 아니므로 [Exception]만 명시적으로 잡아 전파시킨다.
 */
class DailyFortuneItemProcessor(
    private val createDailyFortunePort: CreateDailyFortunePort,
    private val fortuneDate: LocalDate,
) : ItemProcessor<UUID, UUID> {
    override fun process(item: UUID): UUID? =
        try {
            createDailyFortunePort.create(item, fortuneDate)
        } catch (e: Exception) {
            log.error("일일 운세 생성 실패: memberId=$item, fortuneDate=$fortuneDate", e)
            null
        }

    private val log = LoggerFactory.getLogger(DailyFortuneItemProcessor::class.java)
}
