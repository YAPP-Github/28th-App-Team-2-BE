package com.yapp.todakun.shared

/**
 * 알림 종류. 생산자 도메인(오늘의 운세/토닥이 AI/행운 액션 등)이 알림을 발급할 때 지정하며,
 * 도메인 경계를 넘나들기 때문에 shared에 둔다(FortuneCategory와 동일 논리).
 */
enum class NotificationType(
    val classification: NotificationClassification,
) {
    /** 공지 (브로드캐스트) */
    NOTICE(NotificationClassification.SERVICE),

    /** 아침 운 리포트 (오늘의 운세) */
    FORTUNE(NotificationClassification.SERVICE),

    /** 행운 액션 리마인드 */
    LUCKY_ACTION(NotificationClassification.SERVICE),

    /** 토닥이 AI 답변 완료 */
    AI_COMPLETE(NotificationClassification.SERVICE),
}

/**
 * notification.md 1절 분류. SERVICE는 야간에도 무조건 발송 가능하고, MARKETING만 야간 발송 시
 * 별도 동의([com.yapp.todakun.shared.GetPushConsentPort])가 필요하다.
 */
enum class NotificationClassification {
    SERVICE,
    MARKETING,
}
