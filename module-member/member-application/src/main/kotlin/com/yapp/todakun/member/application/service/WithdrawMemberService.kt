package com.yapp.todakun.member.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.member.MemberWithdrawalLog
import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.port.inbound.WithdrawMemberCommand
import com.yapp.todakun.member.port.inbound.WithdrawMemberUseCase
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.member.repository.MemberWithdrawalLogRepository
import com.yapp.todakun.shared.DeleteMemberSajusPort
import com.yapp.todakun.shared.RegisterWithdrawnAccountPort
import com.yapp.todakun.shared.RevokeMemberTokensPort
import kotlin.uuid.ExperimentalUuidApi

/**
 * 회원 탈퇴 유스케이스(탈퇴 정책 v1.0 — 복구 유예 없이 즉시 확정 처리). 한 트랜잭션에서
 * ① 비식별 사유 로그 저장 → ② 소유 사주 데이터 하드 삭제 → ③ 인증 토큰 폐기 →
 * ④ SNS 식별자를 재가입 제한 목록(90일)에 등록 → ⑤ 회원 하드 삭제 순으로 처리한다.
 * 사주·토큰 정리와 재가입 제한 등록은 크로스 도메인 포트로 위임한다(member는 saju·auth 내부 구조를 모른다).
 */
@CommandService
class WithdrawMemberService(
    private val memberRepository: MemberRepository,
    private val memberWithdrawalLogRepository: MemberWithdrawalLogRepository,
    private val deleteMemberSajusPort: DeleteMemberSajusPort,
    private val revokeMemberTokensPort: RevokeMemberTokensPort,
    private val registerWithdrawnAccountPort: RegisterWithdrawnAccountPort,
) : WithdrawMemberUseCase {
    @ExperimentalUuidApi
    override fun withdraw(command: WithdrawMemberCommand) {
        val member = memberRepository.findById(command.memberId) ?: throw MemberNotFoundException()

        memberWithdrawalLogRepository.save(
            MemberWithdrawalLog.create(reason = command.reason, detail = command.detail),
        )
        deleteMemberSajusPort.deleteByMemberId(member.id)
        revokeMemberTokensPort.revokeAll(member.id, command.accessToken)
        registerWithdrawnAccountPort.register(member.oauthProvider, member.providerId)
        memberRepository.deleteById(member.id)
    }
}
