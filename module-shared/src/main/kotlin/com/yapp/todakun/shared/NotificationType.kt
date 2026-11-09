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
 * 알림의 성격 분류. 모든 알림은 발송 시 반드시 둘 중 하나로 태깅한다.
 *
 * - [SERVICE]: 이용자가 신청·기대한 기능 알림. 마케팅 동의가 필요 없고 야간에도 무조건 발송 가능하다.
 * - [MARKETING]: 광고성(이벤트·혜택·프로모션). 마케팅 동의가 필요하고, 야간 발송에는
 *   별도 동의([GetPushConsentPort])가 추가로 필요하다.
 *
 * 새 알림을 정의할 때 분류가 모호하면 보수적으로 [MARKETING]으로 취급한다.
 */
enum class NotificationClassification {
    SERVICE,
    MARKETING,
}
