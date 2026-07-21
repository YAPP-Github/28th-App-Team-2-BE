package com.yapp.todakun.shared

/**
 * 알림 종류. 생산자 도메인(오늘의 운세/토닥이 AI/행운 액션 등)이 알림을 발급할 때 지정하며,
 * 도메인 경계를 넘나들기 때문에 shared에 둔다(FortuneCategory와 동일 논리).
 */
enum class NotificationType {
    /** 공지 (브로드캐스트) */
    NOTICE,

    /** 아침 운 리포트 (오늘의 운세) */
    FORTUNE,

    /** 행운 액션 리마인드 */
    LUCKY_ACTION,

    /** 토닥이 AI 답변 완료 */
    AI_COMPLETE,
}
