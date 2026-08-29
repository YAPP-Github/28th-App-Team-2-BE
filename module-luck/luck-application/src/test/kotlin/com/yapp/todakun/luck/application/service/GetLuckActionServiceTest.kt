package com.yapp.todakun.luck.application.service

import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.fixture.LuckActionFixture
import com.yapp.todakun.luck.repository.LuckActionRepository
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
class GetLuckActionServiceTest :
    DescribeSpec(
        {
            val luckActionRepository = mockk<LuckActionRepository>()
            val getLuckActionService = GetLuckActionService(luckActionRepository)

            afterTest { clearMocks(luckActionRepository) }

            describe("getById") {
                context("존재하는 id이고 소유자가 일치하면") {
                    it("해당 행운 액션을 반환한다") {
                        val luckAction = LuckActionFixture.create()
                        every { luckActionRepository.findById(luckAction.id) } returns luckAction

                        val found = getLuckActionService.getById(luckAction.id, luckAction.memberId)

                        found shouldBe luckAction
                    }
                }

                context("존재하지 않는 id이면") {
                    it("LuckActionNotFoundException을 던진다") {
                        val nonExistentId = Uuid.generateV7().toJavaUuid()
                        val memberId = Uuid.generateV7().toJavaUuid()

                        every { luckActionRepository.findById(nonExistentId) } returns null

                        shouldThrow<LuckActionNotFoundException> {
                            getLuckActionService.getById(nonExistentId, memberId)
                        }
                    }
                }

                context("다른 회원의 행운 액션이면") {
                    it("LuckActionNotFoundException을 던진다") {
                        val luckAction = LuckActionFixture.create()
                        val otherMemberId = Uuid.generateV7().toJavaUuid()

                        every { luckActionRepository.findById(luckAction.id) } returns luckAction

                        shouldThrow<LuckActionNotFoundException> {
                            getLuckActionService.getById(luckAction.id, otherMemberId)
                        }
                    }
                }
            }
        },
    )
