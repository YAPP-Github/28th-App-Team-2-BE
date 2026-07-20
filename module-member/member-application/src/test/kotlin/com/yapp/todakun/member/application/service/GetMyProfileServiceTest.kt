package com.yapp.todakun.member.application.service

import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.repository.MemberRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class GetMyProfileServiceTest : DescribeSpec({
    val memberRepository = mockk<MemberRepository>()
    val service = GetMyProfileService(memberRepository)

    afterTest { clearMocks(memberRepository) }

    describe("getProfile") {
        context("회원이 존재하면") {
            it("회원 정보를 반환한다") {
                val member = MemberFixture.member()
                every { memberRepository.findById(MemberFixture.MEMBER_ID) } returns member

                service.getProfile(MemberFixture.MEMBER_ID) shouldBe member
            }
        }

        context("회원이 존재하지 않으면") {
            it("MemberNotFoundException을 던진다") {
                every { memberRepository.findById(MemberFixture.MEMBER_ID) } returns null

                shouldThrow<MemberNotFoundException> { service.getProfile(MemberFixture.MEMBER_ID) }
            }
        }
    }
})
