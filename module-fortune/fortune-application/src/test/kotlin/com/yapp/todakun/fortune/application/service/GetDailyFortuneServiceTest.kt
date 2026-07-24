package com.yapp.todakun.fortune.application.service

import com.yapp.todakun.fortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.fortune.fixture.DailyFortuneFixture
import com.yapp.todakun.fortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetLuckActionScoresPort
import com.yapp.todakun.shared.LuckActionScore
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
class GetDailyFortuneServiceTest :
    DescribeSpec(
        {
            val dailyFortuneRepository = mockk<DailyFortuneRepository>()
            val getLuckActionScoresPort = mockk<GetLuckActionScoresPort>()
            val getFortuneService = GetDailyFortuneService(dailyFortuneRepository, getLuckActionScoresPort)

            afterTest { clearMocks(dailyFortuneRepository, getLuckActionScoresPort) }

            describe("getById") {
                context("존재하는 id이고 소유자가 일치하면") {
                    it("행운 액션 점수와 함께 오늘의 운세 상세를 반환한다") {
                        val dailyFortune = DailyFortuneFixture.create()
                        val luckActionScores =
                            listOf(
                                LuckActionScore(id = Uuid.generateV7().toJavaUuid(), fortuneCategory = FortuneCategory.MONEY, score = 70),
                            )
                        every { dailyFortuneRepository.findById(dailyFortune.id) } returns dailyFortune
                        every {
                            getLuckActionScoresPort.getScores(dailyFortune.memberId, dailyFortune.fortuneDate)
                        } returns luckActionScores

                        val detail = getFortuneService.getById(dailyFortune.id, dailyFortune.memberId)

                        detail.id shouldBe dailyFortune.id
                        detail.fortuneDate shouldBe dailyFortune.fortuneDate
                        detail.score shouldBe dailyFortune.score
                        detail.title shouldBe dailyFortune.title
                        detail.content shouldBe dailyFortune.content
                        detail.luckyItems shouldBe dailyFortune.luckyItems
                        detail.cautionaryItems shouldBe dailyFortune.cautionaryItems
                        detail.luckActionScores shouldBe luckActionScores
                    }
                }

                context("존재하지 않는 id이면") {
                    it("DailyFortuneNotFoundException을 던진다") {
                        val nonExistentId = Uuid.generateV7().toJavaUuid()
                        val memberId = Uuid.generateV7().toJavaUuid()
                        every { dailyFortuneRepository.findById(nonExistentId) } returns null

                        shouldThrow<DailyFortuneNotFoundException> {
                            getFortuneService.getById(nonExistentId, memberId)
                        }
                    }
                }

                context("다른 회원의 오늘의 운세이면") {
                    it("DailyFortuneNotFoundException을 던진다") {
                        val dailyFortune = DailyFortuneFixture.create()
                        val otherMemberId = Uuid.generateV7().toJavaUuid()
                        every { dailyFortuneRepository.findById(dailyFortune.id) } returns dailyFortune

                        shouldThrow<DailyFortuneNotFoundException> {
                            getFortuneService.getById(dailyFortune.id, otherMemberId)
                        }
                    }
                }
            }
        },
    )
