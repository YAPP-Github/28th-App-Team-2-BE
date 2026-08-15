package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneEmptyResponseException
import com.yapp.todakun.dayfortune.fixture.DaySelectionFortuneFixture
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis
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
                it("날짜별로 한 번씩 위임하고 정렬된 날짜 순서대로 결과를 모은다") {
                    val earlier = LocalDate.now().plusDays(30)
                    val later = LocalDate.now().plusDays(45)
                    val earlierResult = result(earlier)
                    val laterResult = result(later)
                    every { createOneDaySelectionFortuneService.createOne(purpose, earlier, memberId) } returns earlierResult
                    every { createOneDaySelectionFortuneService.createOne(purpose, later, memberId) } returns laterResult

                    val results = service.create(purpose, listOf(later, earlier), memberId)

                    results shouldBe listOf(earlierResult, laterResult)
                    verify(exactly = 1) { createOneDaySelectionFortuneService.createOne(purpose, earlier, memberId) }
                    verify(exactly = 1) { createOneDaySelectionFortuneService.createOne(purpose, later, memberId) }
                }

                it("날짜별 위임을 동시에 진입시켜 병렬로 실행한다") {
                    val dates = listOf(30L, 45L, 60L).map { LocalDate.now().plusDays(it) }
                    val allEntered = CountDownLatch(dates.size)
                    dates.forEach { date ->
                        every { createOneDaySelectionFortuneService.createOne(purpose, date, memberId) } answers {
                            allEntered.countDown()
                            allEntered.await(1, TimeUnit.SECONDS).shouldBeTrue()
                            result(date)
                        }
                    }

                    service.create(purpose, dates, memberId)
                }

                it("날짜별 위임을 병렬로 실행해 순차 실행보다 짧은 시간에 끝낸다") {
                    val dates = listOf(30L, 45L, 60L).map { LocalDate.now().plusDays(it) }
                    val delayMillis = 300L
                    dates.forEach { date ->
                        every { createOneDaySelectionFortuneService.createOne(purpose, date, memberId) } answers {
                            Thread.sleep(delayMillis)
                            result(date)
                        }
                    }

                    val elapsedMillis = measureTimeMillis { service.create(purpose, dates, memberId) }

                    // CI 스케줄링 지연을 감안한 완화된 상한: 완전 순차 실행(dates.size * delayMillis)보다는
                    // 뚜렷하게 짧되, 이상적인 병렬 시간(delayMillis)에 딱 맞출 필요는 없다.
                    elapsedMillis shouldBeLessThan (dates.size - 1) * delayMillis
                }
            }

            context("날짜 중 하나에서 AI 호출이 실패하면") {
                it("발생한 예외를 그대로 전파한다") {
                    val failing = LocalDate.now().plusDays(30)
                    val succeeding = LocalDate.now().plusDays(45)
                    every {
                        createOneDaySelectionFortuneService.createOne(purpose, failing, memberId)
                    } throws DaySelectionFortuneEmptyResponseException()
                    every {
                        createOneDaySelectionFortuneService.createOne(purpose, succeeding, memberId)
                    } returns result(succeeding)

                    shouldThrow<DaySelectionFortuneEmptyResponseException> {
                        service.create(purpose, listOf(failing, succeeding), memberId)
                    }
                }

                it("아직 끝나지 않은 형제 작업의 실행 스레드에 인터럽트를 전달한다") {
                    val failing = LocalDate.now().plusDays(30)
                    val running = LocalDate.now().plusDays(45)
                    val interrupted = AtomicBoolean(false)
                    every {
                        createOneDaySelectionFortuneService.createOne(purpose, failing, memberId)
                    } throws DaySelectionFortuneEmptyResponseException()
                    every {
                        createOneDaySelectionFortuneService.createOne(purpose, running, memberId)
                    } answers {
                        try {
                            Thread.sleep(5_000)
                        } catch (e: InterruptedException) {
                            interrupted.set(true)
                        }
                        result(running)
                    }

                    shouldThrow<DaySelectionFortuneEmptyResponseException> {
                        service.create(purpose, listOf(failing, running), memberId)
                    }

                    interrupted.get().shouldBeTrue()
                }
            }
        }
    })
