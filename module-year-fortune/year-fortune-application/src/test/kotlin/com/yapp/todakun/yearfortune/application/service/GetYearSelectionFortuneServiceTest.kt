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

            describe("getByYear") {
                context("해당 연도별 운세가 있으면") {
                    it("연도별 운세 상세를 반환한다") {
                        val yearSelectionFortune = YearSelectionFortuneFixture.create()
                        every {
                            yearSelectionFortuneRepository.findByMemberIdAndYear(yearSelectionFortune.memberId, yearSelectionFortune.year)
                        } returns yearSelectionFortune

                        val detail = getYearSelectionFortuneService.getByYear(yearSelectionFortune.year, yearSelectionFortune.memberId)

                        detail.id shouldBe yearSelectionFortune.id
                        detail.year shouldBe yearSelectionFortune.year
                        detail.score shouldBe yearSelectionFortune.score
                        detail.title shouldBe yearSelectionFortune.title
                        detail.content shouldBe yearSelectionFortune.content
                        detail.fortuneCategories shouldBe yearSelectionFortune.fortuneCategories
                    }
                }

                context("해당 연도별 운세가 존재하지 않으면") {
                    it("YearSelectionFortuneNotFoundException을 던진다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val year = 2026
                        every { yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) } returns null

                        shouldThrow<YearSelectionFortuneNotFoundException> { getYearSelectionFortuneService.getByYear(year, memberId) }
                    }
                }
            }
        },
    )
