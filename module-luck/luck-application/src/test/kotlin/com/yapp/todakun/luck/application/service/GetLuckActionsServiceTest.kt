package com.yapp.todakun.luck.application.service

import com.yapp.todakun.luck.fixture.LuckActionFixture
import com.yapp.todakun.luck.repository.LuckActionRepository
import com.yapp.todakun.shared.FortuneCategory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class GetLuckActionsServiceTest :
    DescribeSpec(
        {
            val luckActionRepository = mockk<LuckActionRepository>()
            val getLuckActionsService = GetLuckActionsService(luckActionRepository)

            afterTest { clearMocks(luckActionRepository) }

            val memberId = Uuid.generateV7().toJavaUuid()

            describe("getTodayLuckActions") {
                context("오늘 날짜의 행운 액션이 있으면") {
                    it("해당 회원의 오늘 행운 액션 목록을 반환한다") {
                        val luckActions =
                            listOf(
                                LuckActionFixture.create(memberId = memberId, fortuneCategory = FortuneCategory.HEALTH),
                                LuckActionFixture.create(memberId = memberId, fortuneCategory = FortuneCategory.MONEY),
                            )
                        every {
                            luckActionRepository.findAllByMemberIdAndFortuneDate(memberId, any<LocalDate>())
                        } returns luckActions

                        val found = getLuckActionsService.getTodayLuckActions(memberId)

                        found shouldBe luckActions
                    }
                }

                context("오늘 날짜의 행운 액션이 없으면") {
                    it("빈 목록을 반환한다") {
                        every {
                            luckActionRepository.findAllByMemberIdAndFortuneDate(memberId, any<LocalDate>())
                        } returns emptyList()

                        val found = getLuckActionsService.getTodayLuckActions(memberId)

                        found shouldBe emptyList()
                    }
                }
            }
        },
    )
