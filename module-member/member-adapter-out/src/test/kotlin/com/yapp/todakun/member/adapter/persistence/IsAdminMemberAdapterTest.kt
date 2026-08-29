package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.Role
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.repository.MemberRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class IsAdminMemberAdapterTest :
    DescribeSpec(
        {
            val memberRepository = mockk<MemberRepository>()
            val isAdminMemberAdapter = IsAdminMemberAdapter(memberRepository)

            afterTest { clearMocks(memberRepository) }

            describe("isAdmin") {
                context("회원의 role이 ADMIN이면") {
                    it("true를 반환한다") {
                        val member = MemberFixture.create(role = Role.ADMIN)
                        every { memberRepository.findById(member.id) } returns member

                        isAdminMemberAdapter.isAdmin(member.id) shouldBe true
                    }
                }

                context("회원의 role이 MEMBER이면") {
                    it("false를 반환한다") {
                        val member = MemberFixture.create(role = Role.MEMBER)
                        every { memberRepository.findById(member.id) } returns member

                        isAdminMemberAdapter.isAdmin(member.id) shouldBe false
                    }
                }

                context("회원이 존재하지 않으면") {
                    it("false를 반환한다") {
                        val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000099")
                        every { memberRepository.findById(memberId) } returns null

                        isAdminMemberAdapter.isAdmin(memberId) shouldBe false
                    }
                }
            }
        },
    )
