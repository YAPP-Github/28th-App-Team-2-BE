package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.exception.DailyFortuneBatchAlreadyRunningException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.launch.JobRestartException
import org.springframework.batch.core.repository.JobRepository
import java.time.LocalDate

private const val JOB_NAME = "generateDailyFortunesJob"

class RegenerateDailyFortunesBatchServiceTest :
    DescribeSpec({
        val jobOperator = mockk<JobOperator>()
        val jobRepository = mockk<JobRepository>()
        val generateDailyFortunesJob = mockk<Job>()
        val service = RegenerateDailyFortunesBatchService(jobOperator, jobRepository, generateDailyFortunesJob)

        val fortuneDate = LocalDate.of(2026, 6, 24)
        val jobExecution = mockk<JobExecution>()

        every { generateDailyFortunesJob.name } returns JOB_NAME

        afterTest { clearMocks(jobOperator, jobRepository, jobExecution, answers = false) }

        describe("regenerate") {
            context("같은 날짜로 실행 중인 배치가 없으면") {
                it("이전 실행 이력과 무관하게 새 배치를 시작한다") {
                    every { jobRepository.findRunningJobExecutions(JOB_NAME) } returns emptySet()
                    every { jobOperator.start(generateDailyFortunesJob, any<JobParameters>()) } returns jobExecution

                    service.regenerate(fortuneDate)

                    verify(exactly = 1) { jobOperator.start(generateDailyFortunesJob, any<JobParameters>()) }
                }
            }

            context("같은 날짜로 이미 실행 중인 배치가 있으면") {
                it("DailyFortuneBatchAlreadyRunningException을 던지고 배치를 시작하지 않는다") {
                    val runningExecution = mockk<JobExecution>()
                    val runningParameters = mockk<JobParameters>()
                    every { runningExecution.jobParameters } returns runningParameters
                    every { runningParameters.getString("fortuneDate") } returns fortuneDate.toString()
                    every { jobRepository.findRunningJobExecutions(JOB_NAME) } returns setOf(runningExecution)

                    shouldThrow<DailyFortuneBatchAlreadyRunningException> { service.regenerate(fortuneDate) }

                    verify(exactly = 0) { jobOperator.start(any<Job>(), any<JobParameters>()) }
                }
            }

            context("배치 시작 시점에 레이스로 이미 실행 중인 것이 확인되면") {
                it("DailyFortuneBatchAlreadyRunningException을 던진다") {
                    every { jobRepository.findRunningJobExecutions(JOB_NAME) } returns emptySet()
                    every {
                        jobOperator.start(generateDailyFortunesJob, any<JobParameters>())
                    } throws JobRestartException("이미 실행 중")

                    shouldThrow<DailyFortuneBatchAlreadyRunningException> { service.regenerate(fortuneDate) }
                }
            }
        }
    })
