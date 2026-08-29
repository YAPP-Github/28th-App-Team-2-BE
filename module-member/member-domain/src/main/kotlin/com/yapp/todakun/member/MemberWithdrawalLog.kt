package com.yapp.todakun.member

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 회원 탈퇴 사유 로그. 탈퇴 정책 v1.0(4-3)에 따라 **계정 식별 정보와 분리한 비식별 기록**으로,
 * 사유([reason])와 상세([detail])만 서비스 개선·VOC 분석 통계 목적으로 보관한다(회원 식별자 미보관).
 */
data class MemberWithdrawalLog(
    val id: UUID,
    val reason: WithdrawalReason,
    val detail: String?,
) {
    companion object {
        @ExperimentalUuidApi
        fun create(
            reason: WithdrawalReason,
            detail: String?,
        ): MemberWithdrawalLog =
            MemberWithdrawalLog(
                id = Uuid.generateV7().toJavaUuid(),
                reason = reason,
                detail = detail,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            reason: WithdrawalReason,
            detail: String?,
        ): MemberWithdrawalLog =
            MemberWithdrawalLog(
                id = id,
                reason = reason,
                detail = detail,
            )
    }
}
