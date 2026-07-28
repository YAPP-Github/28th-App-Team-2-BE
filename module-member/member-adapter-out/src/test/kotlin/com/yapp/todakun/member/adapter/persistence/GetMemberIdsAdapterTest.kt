package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.repository.MemberRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class GetMemberIdsAdapterTest : DescribeSpec({
    val memberRepository = mockk<MemberRepository>()
    val adapter = GetMemberIdsAdapter(memberRepository)

    afterTest { clearMocks(memberRepository) }

    describe("getMemberIds") {
        context("회원이 존재하면") {
            it("전체 회원 ID 목록을 반환한다") {
                val memberIds = listOf(Uuid.generateV7().toJavaUuid(), Uuid.generateV7().toJavaUuid())
                every { memberRepository.findIds() } returns memberIds

                val result = adapter.getMemberIds()

                result shouldBe memberIds
            }
        }

        context("회원이 존재하지 않으면") {
            it("빈 목록을 반환한다") {
                every { memberRepository.findIds() } returns emptyList()

                val result = adapter.getMemberIds()

                result shouldBe emptyList()
            }
        }
    }
})
