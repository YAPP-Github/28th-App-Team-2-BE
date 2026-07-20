package com.yapp.todakun.member.adapter.persistence

import com.yapp.todakun.member.MemberWithdrawalLog
import com.yapp.todakun.member.repository.MemberWithdrawalLogRepository
import org.springframework.stereotype.Repository

/** 회원 탈퇴 사유 로그 영속 어댑터. */
@Repository
class MemberWithdrawalLogRepositoryAdapter(
    private val memberWithdrawalLogJpaRepository: MemberWithdrawalLogJpaRepository,
) : MemberWithdrawalLogRepository {
    override fun save(log: MemberWithdrawalLog): MemberWithdrawalLog =
        memberWithdrawalLogJpaRepository.save(MemberWithdrawalLogJpaEntity.fromDomain(log)).toDomain()
}
