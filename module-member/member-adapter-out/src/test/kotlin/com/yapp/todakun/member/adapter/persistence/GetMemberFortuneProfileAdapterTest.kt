package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.repository.MemberRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class GetMemberFortuneProfileAdapterTest : DescribeSpec({
    val memberRepository = mockk<MemberRepository>()
    val adapter = GetMemberFortuneProfileAdapter(memberRepository)

    afterTest { clearMocks(memberRepository) }

    val member = MemberFixture.create()

    describe("getProfile") {
        context("존재하는 회원이면") {
            it("운세 생성에 필요한 필드를 MemberFortuneProfile로 변환해 반환한다") {
                every { memberRepository.findById(member.id) } returns member

                val result = adapter.getProfile(member.id)

                result.name shouldBe member.name
                result.birthDate shouldBe member.birthDate
                result.gender shouldBe member.gender.name
                result.job shouldBe member.job.name
                result.relationshipStatus shouldBe member.relationshipStatus.name
            }
        }

        context("존재하지 않는 회원이면") {
            it("MemberNotFoundException을 던진다") {
                every { memberRepository.findById(member.id) } returns null

                shouldThrow<MemberNotFoundException> { adapter.getProfile(member.id) }
            }
        }
    }
})
