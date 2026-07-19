package com.yapp.todakun.terms

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 회원의 약관 동의 내역(사용자 ↔ 약관 중간 테이블). 동의뿐 아니라 미동의([agreed] = false)도
 * 명시적으로 기록해, 이후 알림 발송 대상 필터링 시 "마케팅 거부" 여부를 정확히 판단할 수 있게 한다.
 * (member_id, terms_id)는 유일하며, 재제출 시 [updateDecision]으로 결정을 갱신한다.
 */
data class MemberTermsAgreement(
    val id: UUID,
    val memberId: UUID,
    val termsId: UUID,
    val agreed: Boolean,
) {
    /** 같은 약관에 대한 동의 결정을 갱신한 새 인스턴스를 반환한다(불변). */
    fun updateDecision(agreed: Boolean): MemberTermsAgreement = copy(agreed = agreed)

    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            termsId: UUID,
            agreed: Boolean,
        ): MemberTermsAgreement =
            MemberTermsAgreement(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                termsId = termsId,
                agreed = agreed,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            termsId: UUID,
            agreed: Boolean,
        ): MemberTermsAgreement =
            MemberTermsAgreement(
                id = id,
                memberId = memberId,
                termsId = termsId,
                agreed = agreed,
            )
    }
}
