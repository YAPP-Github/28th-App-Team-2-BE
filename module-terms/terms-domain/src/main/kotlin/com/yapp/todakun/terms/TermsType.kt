package com.yapp.todakun.terms

/**
 * 약관 유형. 특정 약관(마케팅 등)을 UUID 하드코딩 없이 식별하기 위한 안정적인 코드.
 * 이후 알림 도메인이 "마케팅 수신 동의 회원"을 조회할 때 이 유형을 기준으로 분기한다.
 */
enum class TermsType {
    /** 서비스 이용약관(필수). */
    SERVICE,

    /** 개인정보 수집·이용 동의(필수). */
    PRIVACY,

    /** 마케팅 정보 수신 동의(선택). */
    MARKETING,

    /** 야간 광고성 정보 수신 동의(선택). */
    NIGHT_PUSH,
}
