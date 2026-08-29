package com.yapp.todakun.saju

/** 입력 달력 종류. member 도메인과 동일 개념이나, 바운디드 컨텍스트 분리를 위해 saju가 자체 보유한다. */
enum class CalendarType {
    SOLAR, // 양력
    LUNAR, // 음력
}
