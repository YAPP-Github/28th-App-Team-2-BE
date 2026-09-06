package com.yapp.todakun.member.port.inbound

import com.yapp.todakun.member.WithdrawalReason
import java.util.UUID

/**
 * 회원 탈퇴 커맨드. [detail]은 사유가 기타(ETC)일 때의 상세 입력(선택).
 * [jti]·[remainingSeconds]는 탈퇴 요청에 사용된 액세스 토큰의 클레임으로, 탈퇴 시 즉시 블랙리스트에 등록해 무효화한다.
 */
data class WithdrawMemberCommand(
    val memberId: UUID,
    val reason: WithdrawalReason,
    val detail: String?,
    val jti: String,
    val remainingSeconds: Long,
)
