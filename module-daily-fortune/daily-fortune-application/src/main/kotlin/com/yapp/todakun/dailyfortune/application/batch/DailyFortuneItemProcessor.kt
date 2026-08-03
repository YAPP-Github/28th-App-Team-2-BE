package com.yapp.todakun.dailyfortune.application.batch

import com.yapp.todakun.shared.CreateDailyFortunePort
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.batch.core.listener.StepExecutionListener
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemProcessor
import java.time.LocalDate
import java.util.UUID

/**
 * 회원 1명의 오늘의 운세 생성을 처리한다.
 * [CreateDailyFortunePort.create]가 AI 호출부터 저장까지 자체 트랜잭션(@CommandService)으로 커밋하므로,
 * 이 Processor는 별도 상태 없이 결과를 위임만 한다.
 * 회원별 예외는 여기서 격리한다 — null을 반환하면 Spring Batch가 해당 아이템만 필터링하고 다음 회원으로 이어간다.
 * OutOfMemoryError 등 복구 불가능한 [Error]는 격리 대상이 아니므로 [Exception]만 명시적으로 잡아 전파시킨다.
 * fortuneDate는 `#{jobParameters['...']}` SpEL 지연 바인딩 대신 [beforeStep]에서 StepExecution의 JobParameters로 읽는다 —
 * IDE가 SpEL 컨텍스트 변수를 정적으로 해석하지 못해 생기는 오탐(cannot resolve variable)이 애초에 발생하지 않는다.
 * [StepExecutionListener]를 구현하면 SimpleStepBuilder가 processor 등록 시 자동으로 Step 리스너로도 등록한다.
 */
class DailyFortuneItemProcessor(
    private val createDailyFortunePort: CreateDailyFortunePort,
) : ItemProcessor<UUID, UUID>, StepExecutionListener {
    private lateinit var fortuneDate: LocalDate

    override fun beforeStep(stepExecution: StepExecution) {
        fortuneDate = LocalDate.parse(requireNotNull(stepExecution.jobParameters.getString("fortuneDate")))
    }

    override fun process(item: UUID): UUID? =
        try {
            createDailyFortunePort.create(item, fortuneDate)
        } catch (e: Exception) {
            log.error("일일 운세 생성 실패: memberId=$item, fortuneDate=$fortuneDate", e)
            null
        }
}

private val log: Logger = LoggerFactory.getLogger(DailyFortuneItemProcessor::class.java)
