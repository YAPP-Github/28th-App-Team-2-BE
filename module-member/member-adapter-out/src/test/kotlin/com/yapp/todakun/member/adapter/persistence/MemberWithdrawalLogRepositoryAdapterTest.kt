package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.MemberWithdrawalLog
import com.yapp.todakun.member.WithdrawalReason
import com.yapp.todakun.member.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class MemberWithdrawalLogRepositoryAdapterTest(
    private val memberWithdrawalLogJpaRepository: MemberWithdrawalLogJpaRepository,
) : DescribeSpec(
        {
            val adapter = MemberWithdrawalLogRepositoryAdapter(memberWithdrawalLogJpaRepository)
            val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001")

            describe("save") {
                context("탈퇴 사유 로그를 저장하면") {
                    it("저장된 로그를 반환하고 조회된다") {
                        val log = MemberWithdrawalLog.create(memberId, WithdrawalReason.NOT_USING, "자주 안 써요")

                        val saved = adapter.save(log)

                        saved.id shouldBe log.id
                        saved.reason shouldBe WithdrawalReason.NOT_USING
                        memberWithdrawalLogJpaRepository.findById(log.id).isPresent shouldBe true
                    }
                }
            }
        },
    )
