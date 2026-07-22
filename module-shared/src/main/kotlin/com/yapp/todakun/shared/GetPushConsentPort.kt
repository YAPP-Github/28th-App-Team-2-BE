package com.yapp.todakun.shared

import java.util.UUID

/**
 * 알림 발송 대상 회원의 수신 동의 조회 확장점. terms-adapter-out이 구현한다(TermsType.NIGHT_PUSH 기반).
 * 구현 빈이 없으면 발송 측은 "동의"로 간주한다.
 */
interface GetPushConsentPort {
    /** 야간(21~08시) 푸시 수신 동의 여부. */
    fun isNightPushAllowed(memberId: UUID): Boolean
}
