package com.yapp.todakun.luck.application.service

import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.fixture.LuckActionFixture
import com.yapp.todakun.luck.repository.LuckActionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class ToggleLuckActionServiceTest :
    DescribeSpec(
        {
            val luckActionRepository = mockk<LuckActionRepository>()
            val toggleLuckActionService = ToggleLuckActionService(luckActionRepository)

            afterTest { clearMocks(luckActionRepository) }

            describe("toggle") {
                context("존재하는 id이고 소유자가 일치하며 achieved가 false이면") {
                    it("achieved를 true로 변경해 저장하고 반환한다") {
                        val luckAction = LuckActionFixture.create(achieved = false)
                        val savedSlot = slot<LuckAction>()
                        every { luckActionRepository.findById(luckAction.id) } returns luckAction
                        every { luckActionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

                        val result = toggleLuckActionService.toggle(luckAction.id, luckAction.memberId)

                        result.achieved shouldBe true
                        savedSlot.captured.achieved shouldBe true
                        savedSlot.captured.id shouldBe luckAction.id
                    }
                }

                context("achieved가 true인 행운 액션을 다시 toggle하면") {
                    it("achieved를 false로 변경해 저장한다") {
                        val luckAction = LuckActionFixture.create(achieved = true)
                        val savedSlot = slot<LuckAction>()
                        every { luckActionRepository.findById(luckAction.id) } returns luckAction
                        every { luckActionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

                        val result = toggleLuckActionService.toggle(luckAction.id, luckAction.memberId)

                        result.achieved shouldBe false
                        savedSlot.captured.achieved shouldBe false
                    }
                }

                context("존재하지 않는 id이면") {
                    it("LuckActionNotFoundException을 던지고 저장하지 않는다") {
                        val nonExistentId = Uuid.generateV7().toJavaUuid()
                        val memberId = Uuid.generateV7().toJavaUuid()
                        every { luckActionRepository.findById(nonExistentId) } returns null

                        shouldThrow<LuckActionNotFoundException> {
                            toggleLuckActionService.toggle(nonExistentId, memberId)
                        }

                        verify(exactly = 0) { luckActionRepository.save(any()) }
                    }
                }

                context("다른 회원의 행운 액션이면") {
                    it("LuckActionNotFoundException을 던지고 저장하지 않는다") {
                        val luckAction = LuckActionFixture.create()
                        val otherMemberId = Uuid.generateV7().toJavaUuid()
                        every { luckActionRepository.findById(luckAction.id) } returns luckAction

                        shouldThrow<LuckActionNotFoundException> {
                            toggleLuckActionService.toggle(luckAction.id, otherMemberId)
                        }

                        verify(exactly = 0) { luckActionRepository.save(any()) }
                    }
                }
            }
        },
    )
