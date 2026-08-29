package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.dailyfortune.port.inbound.GenerateDailyFortunesUseCase
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecutionException
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * [GenerateDailyFortunesUseCase] 구현체.
 * generateDailyFortunesJob을 fortuneDate를 식별 파라미터로 실어 실행한다.
 * 같은 날짜로 재호출되면 같은 JobInstance를 가리키므로, 이미 완료된 날짜는
 * [org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException]으로 자연스럽게 걸러진다.
 * 실행 자체를 시작하지 못한 경우(이미 실행 중/파라미터 오류 등)는 여기서 로깅하고 삼킨다 —
 * 배치 실행 이후의 회원별 실패는 DailyFortuneJobExecutionListener와 아이템 필터링이 담당한다.
 */
@Service
@Loggable
class GenerateDailyFortunesBatchService(
    private val jobOperator: JobOperator,
    private val generateDailyFortunesJob: Job,
) : GenerateDailyFortunesUseCase {
    override fun generate(fortuneDate: LocalDate) {
        val jobParameters = JobParametersBuilder().addString("fortuneDate", fortuneDate.toString()).toJobParameters()

        try {
            jobOperator.start(generateDailyFortunesJob, jobParameters)
        } catch (e: JobExecutionException) {
            log.error("오늘의 운세 배치를 시작하지 못했습니다: fortuneDate=$fortuneDate", e)
        }
    }
}
