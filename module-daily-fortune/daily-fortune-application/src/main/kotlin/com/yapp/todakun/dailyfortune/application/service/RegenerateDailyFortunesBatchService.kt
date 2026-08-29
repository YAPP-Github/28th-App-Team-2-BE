package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.dailyfortune.exception.DailyFortuneBatchAlreadyRunningException
import com.yapp.todakun.dailyfortune.port.inbound.RegenerateDailyFortunesUseCase
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecutionException
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

private const val FORTUNE_DATE_KEY = "fortuneDate"

/**
 * [RegenerateDailyFortunesUseCase] 구현체.
 * fortuneDate를 identifying 파라미터로 갖되, 호출 시각(requestedAt)도 identifying 파라미터에 함께 실어 매 호출마다
 * 새 JobInstance를 만든다 — 이전 실행이 COMPLETED든 FAILED든 이력과 무관하게 항상 새로 실행 가능하다.
 * [com.yapp.todakun.dailyfortune.application.service.CreateDailyFortuneService.create]가 멱등하므로(이미 생성된
 * 회원은 AI 재호출 없이 스킵) 전체 회원을 다시 훑어도 누락된 회원만 실제로 생성된다.
 * 같은 fortuneDate로 이미 실행 중인 배치가 있으면 중복 AI 호출을 막기 위해 먼저 걸러낸다.
 */
@Service
@Loggable
class RegenerateDailyFortunesBatchService(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    private val generateDailyFortunesJob: Job,
) : RegenerateDailyFortunesUseCase {
    override fun regenerate(fortuneDate: LocalDate) {
        val alreadyRunning =
            jobRepository.findRunningJobExecutions(generateDailyFortunesJob.name).any {
                it.jobParameters.getString(FORTUNE_DATE_KEY) == fortuneDate.toString()
            }
        if (alreadyRunning) {
            log.warn("오늘의 운세 배치가 이미 실행 중입니다: fortuneDate=$fortuneDate")
            throw DailyFortuneBatchAlreadyRunningException()
        }

        val jobParameters =
            JobParametersBuilder()
                .addString(FORTUNE_DATE_KEY, fortuneDate.toString())
                .addLong("requestedAt", System.currentTimeMillis())
                .toJobParameters()

        try {
            jobOperator.start(generateDailyFortunesJob, jobParameters)
        } catch (e: JobExecutionException) {
            log.error("오늘의 운세 배치를 다시 시작하지 못했습니다: fortuneDate=$fortuneDate", e)
            throw DailyFortuneBatchAlreadyRunningException(e)
        }
    }
}
