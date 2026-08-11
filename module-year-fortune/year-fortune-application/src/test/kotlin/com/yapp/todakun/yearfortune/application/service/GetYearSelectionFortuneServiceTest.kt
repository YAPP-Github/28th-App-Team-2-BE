package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneNotFoundException
import com.yapp.todakun.yearfortune.fixture.YearSelectionFortuneFixture
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
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
class GetYearSelectionFortuneServiceTest :
    DescribeSpec(
        {
            val yearSelectionFortuneRepository = mockk<YearSelectionFortuneRepository>()
            val getYearSelectionFortuneService = GetYearSelectionFortuneService(yearSelectionFortuneRepository)

            afterTest { clearMocks(yearSelectionFortuneRepository) }

            describe("getById") {
                context("존재하는 id이고 소유자가 일치하면") {
                    it("연도별 운세를 반환한다") {
                        val yearSelectionFortune = YearSelectionFortuneFixture.create()
                        every { yearSelectionFortuneRepository.findById(yearSelectionFortune.id) } returns yearSelectionFortune

                        val result = getYearSelectionFortuneService.getById(yearSelectionFortune.id, yearSelectionFortune.memberId)

                        result.id shouldBe yearSelectionFortune.id
                        result.year shouldBe yearSelectionFortune.year
                        result.score shouldBe yearSelectionFortune.score
                        result.title shouldBe yearSelectionFortune.title
                        result.content shouldBe yearSelectionFortune.content
                        result.fortuneCategories shouldBe yearSelectionFortune.fortuneCategories
                    }
                }

                context("존재하지 않는 id이면") {
                    it("YearSelectionFortuneNotFoundException을 던진다") {
                        val nonExistentId = Uuid.generateV7().toJavaUuid()
                        val memberId = Uuid.generateV7().toJavaUuid()
                        every { yearSelectionFortuneRepository.findById(nonExistentId) } returns null

                        shouldThrow<YearSelectionFortuneNotFoundException> {
                            getYearSelectionFortuneService.getById(nonExistentId, memberId)
                        }
                    }
                }

                context("다른 회원의 연도별 운세이면") {
                    it("YearSelectionFortuneNotFoundException을 던진다") {
                        val yearSelectionFortune = YearSelectionFortuneFixture.create()
                        val otherMemberId = Uuid.generateV7().toJavaUuid()
                        every { yearSelectionFortuneRepository.findById(yearSelectionFortune.id) } returns yearSelectionFortune

                        shouldThrow<YearSelectionFortuneNotFoundException> {
                            getYearSelectionFortuneService.getById(yearSelectionFortune.id, otherMemberId)
                        }
                    }
                }
            }
        },
    )
