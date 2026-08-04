package com.yapp.todakun.dailyfortune.application.batch

import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import com.yapp.todakun.shared.CreateDailyFortunePort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.step.StepExecution
import java.time.LocalDate
import java.util.UUID

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val DAILY_FORTUNE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")

class DailyFortuneItemProcessorTest :
    DescribeSpec({
        val createDailyFortunePort = mockk<CreateDailyFortunePort>()
        val fortuneDate = LocalDate.of(2026, 6, 24)
        val processor = DailyFortuneItemProcessor(createDailyFortunePort)
        val stepExecution = mockk<StepExecution>()

        every { stepExecution.jobParameters } returns
            JobParametersBuilder().addString("fortuneDate", fortuneDate.toString()).toJobParameters()
        processor.beforeStep(stepExecution)

        afterTest { clearMocks(createDailyFortunePort) }

        describe("process") {
            context("오늘의 운세 생성이 성공하면") {
                it("생성된 id를 반환한다") {
                    every { createDailyFortunePort.create(MEMBER_ID, fortuneDate) } returns DAILY_FORTUNE_ID

                    processor.process(MEMBER_ID) shouldBe DAILY_FORTUNE_ID
                }
            }

            context("오늘의 운세 생성 중 예외가 발생하면") {
                it("잡지 않고 그대로 전파한다") {
                    every { createDailyFortunePort.create(MEMBER_ID, fortuneDate) } throws DailyFortuneGenerationFailedException()

                    shouldThrow<DailyFortuneGenerationFailedException> { processor.process(MEMBER_ID) }
                }
            }
        }
    })
