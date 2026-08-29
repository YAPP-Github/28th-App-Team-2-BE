package com.yapp.todakun.saju.adapter.web.dto.response

import com.yapp.todakun.saju.port.inbound.PartnerSajuSummary
import java.time.LocalDate
import java.util.UUID

/** 상대방 사주 목록 카드 응답. */
data class PartnerSajuSummaryResponse(
    val linkId: UUID,
    val relationshipType: RelationshipTypeResponse?,
    val name: String?,
    val gender: String,
    val birthDate: LocalDate,
    val calendarType: String,
    val birthTime: String,
    val isTimeUnknown: Boolean,
) {
    companion object {
        fun from(summary: PartnerSajuSummary): PartnerSajuSummaryResponse =
            PartnerSajuSummaryResponse(
                linkId = summary.linkId,
                relationshipType = summary.relationshipType?.let { RelationshipTypeResponse.from(it) },
                name = summary.name,
                gender = summary.gender.name,
                birthDate = summary.birthDate,
                calendarType = summary.calendarType.name,
                birthTime = summary.birthTime.name,
                isTimeUnknown = summary.isTimeUnknown,
            )
    }
}
