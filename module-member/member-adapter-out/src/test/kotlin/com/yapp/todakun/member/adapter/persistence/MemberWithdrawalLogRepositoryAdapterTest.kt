package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.MemberWithdrawalLog
import com.yapp.todakun.member.WithdrawalReason
import com.yapp.todakun.member.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
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

            describe("save") {
                context("탈퇴 사유 로그를 저장하면") {
                    it("계정 식별자 없이 사유·상세만 저장하고 조회된다") {
                        val log = MemberWithdrawalLog.create(WithdrawalReason.LOW_USAGE, "자주 안 써요")

                        val saved = adapter.save(log)

                        saved.id shouldBe log.id
                        saved.reason shouldBe WithdrawalReason.LOW_USAGE
                        saved.detail shouldBe "자주 안 써요"
                        memberWithdrawalLogJpaRepository.findById(log.id).isPresent shouldBe true
                    }
                }
            }
        },
    )
