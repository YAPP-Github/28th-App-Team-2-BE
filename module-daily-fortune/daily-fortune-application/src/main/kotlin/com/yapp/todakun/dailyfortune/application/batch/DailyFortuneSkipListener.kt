package com.yapp.todakun.dailyfortune.application.batch

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.batch.core.listener.SkipListener
import org.springframework.batch.core.listener.StepExecutionListener
import org.springframework.batch.core.step.StepExecution
import java.time.LocalDate
import java.util.UUID

/**
 * 재시도(retry)를 모두 소진하고 최종적으로 skip된 회원을 로깅한다.
 * fortuneDate는 [DailyFortuneItemProcessor]와 동일하게 [beforeStep]에서 StepExecution의 JobParameters로 읽는다.
 */
class DailyFortuneSkipListener : SkipListener<UUID, UUID>, StepExecutionListener {
    private lateinit var fortuneDate: LocalDate

    override fun beforeStep(stepExecution: StepExecution) {
        fortuneDate = LocalDate.parse(requireNotNull(stepExecution.jobParameters.getString("fortuneDate")))
    }

    override fun onSkipInProcess(
        item: UUID,
        t: Throwable,
    ) {
        log.error("일일 운세 생성 실패(재시도 소진, skip): memberId=$item, fortuneDate=$fortuneDate", t)
    }
}

private val log: Logger = LoggerFactory.getLogger(DailyFortuneSkipListener::class.java)
