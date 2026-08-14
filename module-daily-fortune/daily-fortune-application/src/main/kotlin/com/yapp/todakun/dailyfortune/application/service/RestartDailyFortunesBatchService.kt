package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.dailyfortune.exception.DailyFortuneBatchNotFoundException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneBatchNotRestartableException
import com.yapp.todakun.dailyfortune.port.inbound.RestartDailyFortunesUseCase
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.launch.JobRestartException
import org.springframework.batch.core.repository.JobRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * [RestartDailyFortunesUseCase] 구현체.
 * [fortuneDate]로 식별되는 JobInstance의 마지막 JobExecution을 찾아 [JobOperator.restart]로 이어서 실행한다.
 * 같은 날짜로 새 실행을 시작(start)하는 것과 달리, 실패한 실행을 재시작하므로 MemberIdItemReader가 저장해둔
 * 마지막 커밋 지점부터 이어서 처리된다 — 이미 성공한 회원은 CreateDailyFortunePort의 멱등성으로 다시 처리되지 않는다.
 * 마지막 실행이 없으면 재시도할 대상이 없는 것이고, 이미 COMPLETED거나 실행 중이면 Spring Batch가
 * [JobRestartException]을 던진다.
 */
@Service
@Loggable
class RestartDailyFortunesBatchService(
    private val jobOperator: JobOperator,
    private val jobRepository: JobRepository,
    private val generateDailyFortunesJob: Job,
) : RestartDailyFortunesUseCase {
    override fun restart(fortuneDate: LocalDate) {
        val jobParameters = JobParametersBuilder().addString("fortuneDate", fortuneDate.toString()).toJobParameters()
        val lastExecution =
            jobRepository.getLastJobExecution(generateDailyFortunesJob.name, jobParameters)
                ?: throw DailyFortuneBatchNotFoundException()

        try {
            jobOperator.restart(lastExecution)
        } catch (e: JobRestartException) {
            log.error("오늘의 운세 배치를 재시도할 수 없습니다: fortuneDate=$fortuneDate", e)
            throw DailyFortuneBatchNotRestartableException(e)
        }
    }
}
