package com.yapp.todakun.saju.port.inbound

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.SajuChartSummary
import java.time.LocalDate
import java.util.UUID

/** 상대방 사주 목록 카드 한 건. 명식 상세 없이 요약 정보(이름·성별·생년·관계 라벨)만 담는다. */
data class PartnerSajuSummary(
    val linkId: UUID,
    val relationshipType: RelationshipType?,
    val name: String?,
    val gender: Gender,
    val calendarType: CalendarType,
    val birthDate: LocalDate,
    val birthTime: BirthTime,
    val isTimeUnknown: Boolean,
) {
    companion object {
        fun from(
            link: MemberSajuLink,
            chart: SajuChartSummary,
        ): PartnerSajuSummary =
            PartnerSajuSummary(
                linkId = link.id,
                relationshipType = link.relationshipType,
                name = chart.name,
                gender = chart.gender,
                calendarType = chart.calendarType,
                birthDate = chart.inputDate,
                birthTime = chart.birthTime,
                isTimeUnknown = chart.isTimeUnknown,
            )
    }
}
