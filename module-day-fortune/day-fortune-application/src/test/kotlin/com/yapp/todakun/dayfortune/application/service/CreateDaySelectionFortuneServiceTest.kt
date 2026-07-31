package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.fixture.DaySelectionFortuneFixture
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class CreateDaySelectionFortuneServiceTest :
    DescribeSpec({
        val createOneDaySelectionFortuneService = mockk<CreateOneDaySelectionFortuneService>()
        val service = CreateDaySelectionFortuneService(createOneDaySelectionFortuneService)

        val memberId = Uuid.generateV7().toJavaUuid()
        val purpose = DaySelectionPurpose.TRAVEL

        afterTest { clearMocks(createOneDaySelectionFortuneService) }

        fun result(targetDate: LocalDate): DaySelectionFortuneResult =
            DaySelectionFortuneResult.from(
                DaySelectionFortuneFixture.create(memberId = memberId, purpose = purpose, targetDate = targetDate),
            )

        describe("create") {
            context("후보 날짜에 중복이 있으면") {
                it("중복을 제거하고 날짜당 한 번만 위임한다") {
                    val date = LocalDate.now().plusDays(30)
                    every { createOneDaySelectionFortuneService.createOne(purpose, date, memberId) } returns result(date)

                    val results = service.create(purpose, listOf(date, date), memberId)

                    results.size shouldBe 1
                    verify(exactly = 1) { createOneDaySelectionFortuneService.createOne(purpose, date, memberId) }
                }
            }

            context("후보 날짜가 여러 건이면") {
                it("정렬된 순서대로 날짜별로 위임하고 반환된 순서대로 결과를 모은다") {
                    val earlier = LocalDate.now().plusDays(30)
                    val later = LocalDate.now().plusDays(45)
                    val earlierResult = result(earlier)
                    val laterResult = result(later)
                    every { createOneDaySelectionFortuneService.createOne(purpose, earlier, memberId) } returns earlierResult
                    every { createOneDaySelectionFortuneService.createOne(purpose, later, memberId) } returns laterResult

                    val results = service.create(purpose, listOf(later, earlier), memberId)

                    results shouldBe listOf(earlierResult, laterResult)
                    verifyOrder {
                        createOneDaySelectionFortuneService.createOne(purpose, earlier, memberId)
                        createOneDaySelectionFortuneService.createOne(purpose, later, memberId)
                    }
                }
            }
        }
    })
