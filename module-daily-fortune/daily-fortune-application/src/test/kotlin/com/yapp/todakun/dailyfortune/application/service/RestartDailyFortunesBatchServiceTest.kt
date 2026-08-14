package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.exception.DailyFortuneBatchNotFoundException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneBatchNotRestartableException
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

class RestartDailyFortunesBatchServiceTest :
    DescribeSpec({
        val jobOperator = mockk<JobOperator>()
        val jobRepository = mockk<JobRepository>()
        val generateDailyFortunesJob = mockk<Job>()
        val service = RestartDailyFortunesBatchService(jobOperator, jobRepository, generateDailyFortunesJob)

        val fortuneDate = LocalDate.of(2026, 6, 24)
        val jobExecution = mockk<JobExecution>()

        every { generateDailyFortunesJob.name } returns JOB_NAME

        afterTest { clearMocks(jobOperator, jobRepository, jobExecution, answers = false) }

        describe("restart") {
            context("해당 날짜로 실행된 배치 이력이 있으면") {
                it("마지막 실행을 재시도한다") {
                    every { jobRepository.getLastJobExecution(JOB_NAME, any<JobParameters>()) } returns jobExecution
                    every { jobOperator.restart(jobExecution) } returns jobExecution

                    service.restart(fortuneDate)

                    verify(exactly = 1) { jobOperator.restart(jobExecution) }
                }
            }

            context("해당 날짜로 실행된 배치 이력이 없으면") {
                it("DailyFortuneBatchNotFoundException을 던진다") {
                    every { jobRepository.getLastJobExecution(JOB_NAME, any<JobParameters>()) } returns null

                    shouldThrow<DailyFortuneBatchNotFoundException> { service.restart(fortuneDate) }

                    verify(exactly = 0) { jobOperator.restart(any<JobExecution>()) }
                }
            }

            context("마지막 실행이 이미 완료됐거나 실행 중이면") {
                it("DailyFortuneBatchNotRestartableException을 던진다") {
                    every { jobRepository.getLastJobExecution(JOB_NAME, any<JobParameters>()) } returns jobExecution
                    every { jobOperator.restart(jobExecution) } throws JobRestartException("이미 완료됨")

                    shouldThrow<DailyFortuneBatchNotRestartableException> { service.restart(fortuneDate) }
                }
            }
        }
    })
