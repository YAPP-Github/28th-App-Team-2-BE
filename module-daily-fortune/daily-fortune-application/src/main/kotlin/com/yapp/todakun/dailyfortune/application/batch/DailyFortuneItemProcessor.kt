package com.yapp.todakun.dailyfortune.application.batch

import com.yapp.todakun.shared.CreateDailyFortunePort
import org.springframework.batch.core.listener.StepExecutionListener
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemProcessor
import java.time.LocalDate
import java.util.UUID

/**
 * 회원 1명의 오늘의 운세 생성을 처리한다.
 * [CreateDailyFortunePort.create]가 AI 호출부터 저장까지 자체 트랜잭션(@CommandService)으로 커밋하므로,
 * 이 Processor는 별도 상태 없이 결과를 위임만 한다.
 * 예외는 여기서 잡지 않고 그대로 전파한다 — Step에 걸린 재시도(faultTolerant + retry) 정책이 재시도를 소진한 뒤에도
 * 실패하면 skip 정책이 해당 회원만 건너뛰고 다음 회원으로 이어간다([GenerateDailyFortunesJobConfig] 참고).
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

    override fun process(item: UUID): UUID = createDailyFortunePort.create(item, fortuneDate)
}
