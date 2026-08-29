package com.yapp.todakun.dailyfortune.application.batch

import com.yapp.todakun.common.logging.Loggable
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.listener.JobExecutionListener
import java.time.Duration
import java.time.LocalDateTime

/**
 * 오늘의 운세 배치 Job의 시작/종료를 로깅한다.
 * Step이 하나뿐이므로 그 StepExecution의 read/write/skip count로 처리 현황을 요약한다.
 * BATCH_JOB_EXECUTION 메타데이터로도 조회 가능하지만, 새벽 배치 실패를 사람이 로그만으로 바로 알아차릴 수 있게 한다.
 */
@Loggable
class DailyFortuneJobExecutionListener : JobExecutionListener {
    override fun beforeJob(jobExecution: JobExecution) {
        log.info("오늘의 운세 배치 시작: jobParameters=${jobExecution.jobParameters}")
    }

    override fun afterJob(jobExecution: JobExecution) {
        val startTime = jobExecution.startTime ?: LocalDateTime.now()
        val endTime = jobExecution.endTime ?: LocalDateTime.now()
        val stepExecution = jobExecution.stepExecutions.firstOrNull()

        log.info(
            "오늘의 운세 배치 종료: status={}, duration={}s, readCount={}, writeCount={}, skipCount={}",
            jobExecution.status,
            Duration.between(startTime, endTime).seconds,
            stepExecution?.readCount,
            stepExecution?.writeCount,
            stepExecution?.skipCount,
        )
    }
}
