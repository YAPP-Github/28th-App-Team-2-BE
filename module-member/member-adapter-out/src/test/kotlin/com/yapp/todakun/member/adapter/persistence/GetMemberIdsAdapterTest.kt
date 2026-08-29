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
            it("커서 이후 회원 ID 목록을 페이지 크기만큼 반환한다") {
                val afterMemberId = Uuid.generateV7().toJavaUuid()
                val memberIds = listOf(Uuid.generateV7().toJavaUuid(), Uuid.generateV7().toJavaUuid())
                every { memberRepository.findIdsAfter(afterMemberId, 2) } returns memberIds

                val result = adapter.getMemberIds(afterMemberId, 2)

                result shouldBe memberIds
            }
        }

        context("회원이 존재하지 않으면") {
            it("빈 목록을 반환한다") {
                every { memberRepository.findIdsAfter(null, 2) } returns emptyList()

                val result = adapter.getMemberIds(null, 2)

                result shouldBe emptyList()
            }
        }
    }
})
