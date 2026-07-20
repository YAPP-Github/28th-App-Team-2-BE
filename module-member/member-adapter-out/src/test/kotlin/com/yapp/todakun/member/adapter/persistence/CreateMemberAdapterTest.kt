package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.Member
import com.yapp.todakun.member.exception.MemberProfileInvalidException
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.repository.MemberRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class CreateMemberAdapterTest :
    DescribeSpec(
        {
            val memberRepository = mockk<MemberRepository>()
            val createMemberAdapter = CreateMemberAdapter(memberRepository)

            afterTest { clearMocks(memberRepository) }

            val member = MemberFixture.create()

            describe("create") {
                context("모든 필드가 유효한 값이면") {
                    it("문자열을 enum으로 변환해 회원을 저장하고 저장된 회원의 id를 반환한다") {
                        val memberSlot = slot<Member>()
                        every { memberRepository.save(capture(memberSlot)) } returns member

                        val id =
                            createMemberAdapter.create(
                                provider = member.oauthProvider,
                                providerId = member.providerId,
                                name = member.name,
                                birthDate = member.birthDate,
                                birthTime = member.birthTime.name,
                                calendarType = member.calendarType.name,
                                gender = member.gender.name,
                                job = member.job.name,
                                relationshipStatus = member.relationshipStatus.name,
                                favoriteFortuneCategories = member.favoriteFortuneCategories.map { it.name },
                            )

                        id shouldBe member.id
                        memberSlot.captured.name shouldBe member.name
                        memberSlot.captured.birthDate shouldBe member.birthDate
                        memberSlot.captured.birthTime shouldBe member.birthTime
                        memberSlot.captured.calendarType shouldBe member.calendarType
                        memberSlot.captured.gender shouldBe member.gender
                        memberSlot.captured.oauthProvider shouldBe member.oauthProvider
                        memberSlot.captured.providerId shouldBe member.providerId
                        memberSlot.captured.job shouldBe member.job
                        memberSlot.captured.relationshipStatus shouldBe member.relationshipStatus
                        memberSlot.captured.favoriteFortuneCategories shouldBe member.favoriteFortuneCategories
                    }
                }

                context("enum으로 변환할 수 없는 값이 포함되어 있으면") {
                    it("MemberProfileInvalidException을 던진다") {
                        shouldThrow<MemberProfileInvalidException> {
                            createMemberAdapter.create(
                                provider = member.oauthProvider,
                                providerId = member.providerId,
                                name = member.name,
                                birthDate = member.birthDate,
                                birthTime = member.birthTime.name,
                                calendarType = member.calendarType.name,
                                gender = "INVALID",
                                job = member.job.name,
                                relationshipStatus = member.relationshipStatus.name,
                                favoriteFortuneCategories = member.favoriteFortuneCategories.map { it.name },
                            )
                        }

                        verify(exactly = 0) { memberRepository.save(any()) }
                    }
                }
            }
        },
    )
