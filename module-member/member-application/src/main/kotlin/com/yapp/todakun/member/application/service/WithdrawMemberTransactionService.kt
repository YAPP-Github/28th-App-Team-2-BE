package com.yapp.todakun.member.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.member.MemberWithdrawalLog
import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.port.inbound.WithdrawMemberCommand
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.member.repository.MemberWithdrawalLogRepository
import com.yapp.todakun.shared.DeleteMemberSajusPort
import com.yapp.todakun.shared.OauthRevokeCredential
import com.yapp.todakun.shared.RegisterWithdrawnAccountPort
import com.yapp.todakun.shared.RevokeMemberTokensPort
import com.yapp.todakun.shared.RevokeOauthTokenPort
import kotlin.uuid.ExperimentalUuidApi

/**
 * [WithdrawMemberService]의 트랜잭션 경계. 한 트랜잭션에서
 * ① 비식별 사유 로그 저장 → ② 소유 사주 데이터 하드 삭제 → ③ 인증 토큰 폐기 →
 * ④ SNS 식별자를 재가입 제한 목록(90일)에 등록 → ⑤ (Apple만) 로컬 OAuth 자격증명 삭제 →
 * ⑥ 회원 하드 삭제 순으로 처리한다.
 * Apple 쪽 실제 revoke 호출(외부 HTTP, 최대 8초)은 이 트랜잭션에 포함하지 않는다 — 커밋 전에 나가면
 * 이후 단계가 롤백돼도 되돌릴 수 없고, 커밋될 때까지 DB 커넥션을 불필요하게 점유하게 된다.
 * 로컬 자격증명 삭제까지는 이 트랜잭션에서 원자적으로 처리하고, 커밋이 끝난 뒤
 * [WithdrawMemberService]가 반환된 자격증명으로 revoke API만 별도 호출한다.
 */
@CommandService
class WithdrawMemberTransactionService(
    private val memberRepository: MemberRepository,
    private val memberWithdrawalLogRepository: MemberWithdrawalLogRepository,
    private val deleteMemberSajusPort: DeleteMemberSajusPort,
    private val revokeMemberTokensPort: RevokeMemberTokensPort,
    private val registerWithdrawnAccountPort: RegisterWithdrawnAccountPort,
    private val revokeOauthTokenPort: RevokeOauthTokenPort,
) {
    @ExperimentalUuidApi
    fun withdraw(command: WithdrawMemberCommand): OauthRevokeCredential? {
        val member = memberRepository.findById(command.memberId) ?: throw MemberNotFoundException()

        memberWithdrawalLogRepository.save(
            MemberWithdrawalLog.create(reason = command.reason, detail = command.detail),
        )
        deleteMemberSajusPort.deleteByMemberId(member.id)
        revokeMemberTokensPort.revokeAll(member.id, command.accessToken)
        registerWithdrawnAccountPort.register(member.oauthProvider, member.providerId)
        val oauthRevokeCredential = revokeOauthTokenPort.prepareRevoke(member.oauthProvider, member.providerId)
        memberRepository.deleteById(member.id)

        return oauthRevokeCredential
    }
}
