package com.yapp.todakun.dailyfortune.application.batch

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.step.StepExecution
import java.time.LocalDateTime

class DailyFortuneJobExecutionListenerTest :
    DescribeSpec({
        val jobExecution = mockk<JobExecution>()
        val stepExecution = mockk<StepExecution>()
        val listener = DailyFortuneJobExecutionListener()

        afterTest { clearMocks(jobExecution, stepExecution) }

        describe("beforeJob") {
            context("Job이 시작되면") {
                it("예외 없이 로깅한다") {
                    every { jobExecution.jobParameters } returns JobParameters()

                    shouldNotThrowAny { listener.beforeJob(jobExecution) }
                }
            }
        }

        describe("afterJob") {
            context("Job이 정상 종료되면") {
                it("소요 시간과 처리 현황을 예외 없이 로깅한다") {
                    every { jobExecution.status } returns BatchStatus.COMPLETED
                    every { jobExecution.startTime } returns LocalDateTime.of(2026, 6, 24, 3, 0)
                    every { jobExecution.endTime } returns LocalDateTime.of(2026, 6, 24, 3, 12)
                    every { jobExecution.stepExecutions } returns listOf(stepExecution)
                    every { stepExecution.readCount } returns 10L
                    every { stepExecution.writeCount } returns 8L
                    every { stepExecution.skipCount } returns 2L

                    shouldNotThrowAny { listener.afterJob(jobExecution) }
                }
            }

            context("startTime/endTime이 아직 기록되지 않았으면") {
                it("현재 시각으로 대체해 예외 없이 로깅한다") {
                    every { jobExecution.status } returns BatchStatus.FAILED
                    every { jobExecution.startTime } returns null
                    every { jobExecution.endTime } returns null
                    every { jobExecution.stepExecutions } returns emptyList()

                    shouldNotThrowAny { listener.afterJob(jobExecution) }
                }
            }
        }
    })
