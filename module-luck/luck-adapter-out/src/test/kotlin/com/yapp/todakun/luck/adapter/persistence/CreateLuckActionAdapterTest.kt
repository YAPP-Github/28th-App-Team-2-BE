package com.yapp.todakun.luck.adapter.persistence

import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.exception.LuckActionContentTooLongException
import com.yapp.todakun.luck.exception.LuckActionTitleTooLongException
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
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class CreateLuckActionAdapterTest :
    DescribeSpec(
        {
            val luckActionRepository = mockk<LuckActionRepository>()
            val createLuckActionAdapter = CreateLuckActionAdapter(luckActionRepository)

            afterTest { clearMocks(luckActionRepository) }

            val luckAction = LuckActionFixture.create()

            fun createLuckAction(
                title: String = luckAction.title,
                content: String = luckAction.content,
            ): UUID =
                createLuckActionAdapter.create(
                    memberId = luckAction.memberId,
                    fortuneCategory = luckAction.fortuneCategory,
                    fortuneDate = luckAction.fortuneDate,
                    score = luckAction.score,
                    title = title,
                    content = content,
                )

            describe("create") {
                context("제목과 내용 길이가 모두 유효하면") {
                    it("행운 액션을 저장하고 저장된 행운 액션의 id를 반환한다") {
                        val luckActionSlot = slot<LuckAction>()
                        every { luckActionRepository.save(capture(luckActionSlot)) } returns luckAction

                        val id = createLuckAction()

                        id shouldBe luckAction.id
                        luckActionSlot.captured.memberId shouldBe luckAction.memberId
                        luckActionSlot.captured.fortuneCategory shouldBe luckAction.fortuneCategory
                        luckActionSlot.captured.fortuneDate shouldBe luckAction.fortuneDate
                        luckActionSlot.captured.score shouldBe luckAction.score
                        luckActionSlot.captured.title shouldBe luckAction.title
                        luckActionSlot.captured.content shouldBe luckAction.content
                        luckActionSlot.captured.achieved shouldBe false
                    }
                }

                context("제목 길이가 30자를 초과하면") {
                    it("LuckActionTitleTooLongException을 던진다") {
                        val tooLongTitle = "가".repeat(31)

                        shouldThrow<LuckActionTitleTooLongException> { createLuckAction(title = tooLongTitle) }

                        verify(exactly = 0) { luckActionRepository.save(any()) }
                    }
                }

                context("내용 길이가 200자를 초과하면") {
                    it("LuckActionContentTooLongException을 던진다") {
                        val tooLongContent = "가".repeat(201)

                        shouldThrow<LuckActionContentTooLongException> { createLuckAction(content = tooLongContent) }

                        verify(exactly = 0) { luckActionRepository.save(any()) }
                    }
                }
            }
        },
    )
