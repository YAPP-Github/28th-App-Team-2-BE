package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.OauthProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class GetMemberAdapterTest :
    DescribeSpec(
        {
            val memberRepository = mockk<MemberRepository>()
            val getMemberAdapter = GetMemberAdapter(memberRepository)

            afterTest { clearMocks(memberRepository) }

            describe("findIdByOauth") {
                context("일치하는 회원이 존재하면") {
                    it("해당 회원의 id를 반환한다") {
                        val member = MemberFixture.create()
                        every { memberRepository.findIdByOauth(member.oauthProvider, member.providerId) } returns member.id

                        val found = getMemberAdapter.findIdByOauth(member.oauthProvider, member.providerId)

                        found shouldBe member.id
                    }
                }

                context("일치하는 회원이 없으면") {
                    it("null을 반환한다") {
                        val member = MemberFixture.create(oauthProvider = OauthProvider.KAKAO)
                        every { memberRepository.findIdByOauth(member.oauthProvider, member.providerId) } returns null

                        val found = getMemberAdapter.findIdByOauth(member.oauthProvider, member.providerId)

                        found.shouldBeNull()
                    }
                }
            }
        },
    )
