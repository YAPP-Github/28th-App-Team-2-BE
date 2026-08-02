package com.yapp.todakun.member.application.service

import com.yapp.todakun.member.port.inbound.WithdrawMemberCommand
import com.yapp.todakun.member.port.inbound.WithdrawMemberUseCase
import com.yapp.todakun.shared.RevokeOauthTokenPort
import org.springframework.stereotype.Service
import kotlin.uuid.ExperimentalUuidApi

/**
 * 회원 탈퇴 유스케이스(탈퇴 정책 v1.0 — 복구 유예 없이 즉시 확정 처리).
 * [WithdrawMemberTransactionService]가 로그 저장·사주 삭제·토큰 폐기·재가입 제한 등록·회원 하드 삭제를
 * 한 트랜잭션으로 커밋한 뒤, (Apple만) OAuth 토큰 철회를 트랜잭션 밖에서 호출한다.
 */
@Service
class WithdrawMemberService(
    private val withdrawMemberTransactionService: WithdrawMemberTransactionService,
    private val revokeOauthTokenPort: RevokeOauthTokenPort,
) : WithdrawMemberUseCase {
    @ExperimentalUuidApi
    override fun withdraw(command: WithdrawMemberCommand) {
        val member = withdrawMemberTransactionService.withdraw(command)
        revokeOauthTokenPort.revokeIfApplicable(member.oauthProvider, member.providerId)
    }
}
