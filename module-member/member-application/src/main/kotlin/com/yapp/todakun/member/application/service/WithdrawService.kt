package com.yapp.todakun.member.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.member.MemberWithdrawalLog
import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.port.inbound.WithdrawCommand
import com.yapp.todakun.member.port.inbound.WithdrawUseCase
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.member.repository.MemberWithdrawalLogRepository
import com.yapp.todakun.shared.DeleteMemberSajuDataPort
import com.yapp.todakun.shared.RevokeMemberTokensPort
import kotlin.uuid.ExperimentalUuidApi

/**
 * 회원 탈퇴 유스케이스(하드 삭제). 한 트랜잭션에서 사유 로그를 남기고 회원이 소유한 사주 데이터·인증 토큰을 정리한 뒤
 * 회원을 삭제한다. 사주/토큰 정리는 크로스 도메인 포트로 위임한다(member는 saju·auth 내부 구조를 모른다).
 */
@CommandService
class WithdrawService(
    private val memberRepository: MemberRepository,
    private val memberWithdrawalLogRepository: MemberWithdrawalLogRepository,
    private val deleteMemberSajuDataPort: DeleteMemberSajuDataPort,
    private val revokeMemberTokensPort: RevokeMemberTokensPort,
) : WithdrawUseCase {
    @ExperimentalUuidApi
    override fun withdraw(command: WithdrawCommand) {
        val member = memberRepository.findById(command.memberId) ?: throw MemberNotFoundException()

        memberWithdrawalLogRepository.save(
            MemberWithdrawalLog.create(memberId = member.id, reason = command.reason, detail = command.detail),
        )
        deleteMemberSajuDataPort.deleteByMemberId(member.id)
        revokeMemberTokensPort.revokeAll(member.id)
        memberRepository.deleteById(member.id)
    }
}
