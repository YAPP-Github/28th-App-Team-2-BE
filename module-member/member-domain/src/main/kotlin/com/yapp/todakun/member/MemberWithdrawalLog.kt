package com.yapp.todakun.member

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 회원 탈퇴 사유 로그. 하드 삭제된 회원의 [memberId]는 참조 무결성 없는 단순 기록용이며,
 * 개인정보가 아닌 사유([reason])·상세([detail])만 통계 목적으로 보관한다.
 */
data class MemberWithdrawalLog(
    val id: UUID,
    val memberId: UUID,
    val reason: WithdrawalReason,
    val detail: String?,
) {
    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            reason: WithdrawalReason,
            detail: String?,
        ): MemberWithdrawalLog =
            MemberWithdrawalLog(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                reason = reason,
                detail = detail,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            reason: WithdrawalReason,
            detail: String?,
        ): MemberWithdrawalLog =
            MemberWithdrawalLog(
                id = id,
                memberId = memberId,
                reason = reason,
                detail = detail,
            )
    }
}
