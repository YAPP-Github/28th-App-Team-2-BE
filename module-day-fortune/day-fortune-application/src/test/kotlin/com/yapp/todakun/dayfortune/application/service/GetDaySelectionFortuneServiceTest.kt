package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneNotFoundException
import com.yapp.todakun.dayfortune.fixture.DaySelectionFortuneFixture
import com.yapp.todakun.dayfortune.repository.DaySelectionFortuneRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class GetDaySelectionFortuneServiceTest :
    DescribeSpec(
        {
            val daySelectionFortuneRepository = mockk<DaySelectionFortuneRepository>()
            val getDaySelectionFortuneService = GetDaySelectionFortuneService(daySelectionFortuneRepository)

            afterTest { clearMocks(daySelectionFortuneRepository) }

            describe("getById") {
                context("존재하는 id이고 소유자가 일치하면") {
                    it("택일 운세를 반환한다") {
                        val daySelectionFortune = DaySelectionFortuneFixture.create()
                        every { daySelectionFortuneRepository.findById(daySelectionFortune.id) } returns daySelectionFortune

                        val result = getDaySelectionFortuneService.getById(daySelectionFortune.id, daySelectionFortune.memberId)

                        result.id shouldBe daySelectionFortune.id
                        result.purpose shouldBe daySelectionFortune.purpose
                        result.targetDate shouldBe daySelectionFortune.targetDate
                        result.score shouldBe daySelectionFortune.score
                        result.title shouldBe daySelectionFortune.title
                        result.content shouldBe daySelectionFortune.content
                        result.fortuneCategories shouldBe daySelectionFortune.fortuneCategories
                    }
                }

                context("존재하지 않는 id이면") {
                    it("DaySelectionFortuneNotFoundException을 던진다") {
                        val nonExistentId = Uuid.generateV7().toJavaUuid()
                        val memberId = Uuid.generateV7().toJavaUuid()
                        every { daySelectionFortuneRepository.findById(nonExistentId) } returns null

                        shouldThrow<DaySelectionFortuneNotFoundException> {
                            getDaySelectionFortuneService.getById(nonExistentId, memberId)
                        }
                    }
                }

                context("다른 회원의 택일 운세이면") {
                    it("DaySelectionFortuneNotFoundException을 던진다") {
                        val daySelectionFortune = DaySelectionFortuneFixture.create()
                        val otherMemberId = Uuid.generateV7().toJavaUuid()
                        every { daySelectionFortuneRepository.findById(daySelectionFortune.id) } returns daySelectionFortune

                        shouldThrow<DaySelectionFortuneNotFoundException> {
                            getDaySelectionFortuneService.getById(daySelectionFortune.id, otherMemberId)
                        }
                    }
                }
            }
        },
    )
