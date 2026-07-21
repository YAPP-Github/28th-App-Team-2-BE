package com.yapp.todakun.member

/**
 * 회원 탈퇴 사유(단일 선택, 필수). 계정 식별 정보와 분리해 비식별 상태로 보관하며 서비스 개선·VOC 분석 통계에만 활용한다.
 * [label]은 화면 표시용 문구(탈퇴 정책 v1.0 선택지 9종).
 */
enum class WithdrawalReason(
    val label: String,
) {
    CONTENT_INAPPROPRIATE("콘텐츠가 부적절해요 (사주/운세 결과가 마음에 안 들어요)"),
    CHATBOT_UNSATISFACTORY("토닥이(AI 챗봇) 답변이 만족스럽지 않아요"),
    LOW_USAGE("사용 빈도가 낮아요 (자주 이용하지 않아요)"),
    MISSING_FEATURE("원하는 기능이 없어요"),
    PAYMENT_INCONVENIENCE("유료 결제/과금 방식이 불편해요"),
    PRIVACY_CONCERN("개인정보 제공이 불안해요"),
    FREQUENT_ERRORS("오류가 잦아요 (버그, 앱이 느려요)"),
    SWITCHING_SERVICE("다른 앱/서비스로 이동해요"),
    ETC("기타"),
}
