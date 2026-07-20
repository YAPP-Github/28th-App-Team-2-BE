package com.yapp.todakun.member.port.inbound

import com.yapp.todakun.member.WithdrawalReason
import java.util.UUID

/** 회원 탈퇴 커맨드. [detail]은 사유가 기타(ETC)일 때의 상세 입력(선택). */
data class WithdrawCommand(
    val memberId: UUID,
    val reason: WithdrawalReason,
    val detail: String?,
)
