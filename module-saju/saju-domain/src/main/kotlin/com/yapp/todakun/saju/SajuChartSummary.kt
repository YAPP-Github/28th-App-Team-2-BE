package com.yapp.todakun.saju

import java.time.LocalDate
import java.util.UUID

/** 사주 명식 요약(목록 카드용). 4주·오행·십성 상세 없이 헤더 컬럼만 담는다([SajuChart]의 경량 조회 결과). */
data class SajuChartSummary(
    val id: UUID,
    val name: String?,
    val gender: Gender,
    val calendarType: CalendarType,
    val inputDate: LocalDate,
    val birthTime: BirthTime,
    val isTimeUnknown: Boolean,
)
