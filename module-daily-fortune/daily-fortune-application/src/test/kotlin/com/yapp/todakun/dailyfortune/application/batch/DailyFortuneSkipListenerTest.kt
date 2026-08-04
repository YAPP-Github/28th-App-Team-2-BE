package com.yapp.todakun.dailyfortune.application.batch

import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.step.StepExecution
import java.time.LocalDate
import java.util.UUID

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")

class DailyFortuneSkipListenerTest :
    DescribeSpec({
        val fortuneDate = LocalDate.of(2026, 6, 24)
        val listener = DailyFortuneSkipListener()
        val stepExecution = mockk<StepExecution>()

        every { stepExecution.jobParameters } returns
            JobParametersBuilder().addString("fortuneDate", fortuneDate.toString()).toJobParameters()
        listener.beforeStep(stepExecution)

        describe("onSkipInProcess") {
            context("재시도를 소진하고 최종 skip되면") {
                it("예외 없이 로깅한다") {
                    shouldNotThrowAny {
                        listener.onSkipInProcess(MEMBER_ID, DailyFortuneGenerationFailedException())
                    }
                }
            }
        }
    })
