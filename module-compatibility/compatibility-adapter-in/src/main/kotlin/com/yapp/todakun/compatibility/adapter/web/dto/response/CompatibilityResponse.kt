package com.yapp.todakun.compatibility.adapter.web.dto.response

import com.yapp.todakun.compatibility.CompatibilityElement
import com.yapp.todakun.compatibility.CompatibilityOhaeng
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.port.inbound.CompatibilityResult
import java.util.UUID

/** 궁합 생성 결과 응답. [ohaengs]는 오행 시각화(5개, 합계 100)이고, 사주원국(4주)은 포함하지 않는다. */
data class CompatibilityResponse(
    val id: UUID,
    val partnerName: String?,
    val relationshipType: RelationshipTypeResponse,
    val score: Int,
    val headline: String,
    val subheadline: String,
    val summary: String,
    val totalAnalysis: String,
    val analysisBasis: String,
    val ohaengs: List<CompatibilityOhaengResponse>,
) {
    companion object {
        fun from(result: CompatibilityResult): CompatibilityResponse =
            CompatibilityResponse(
                id = result.id,
                partnerName = result.partnerName,
                relationshipType = RelationshipTypeResponse.from(result.relationshipType),
                score = result.score,
                headline = result.headline,
                subheadline = result.subheadline,
                summary = result.summary,
                totalAnalysis = result.totalAnalysis,
                analysisBasis = result.analysisBasis,
                ohaengs = result.ohaengs.map(CompatibilityOhaengResponse::from),
            )
    }
}

/** 관계 유형 표시 정보(코드·한글). */
data class RelationshipTypeResponse(
    val code: String,
    val label: String,
) {
    companion object {
        fun from(type: CompatibilityRelationshipType): RelationshipTypeResponse = RelationshipTypeResponse(type.name, type.label)
    }
}

/** 오행 시각화 한 행(오행·정수 비율). */
data class CompatibilityOhaengResponse(
    val element: ElementResponse,
    val percentage: Int,
) {
    companion object {
        fun from(ohaeng: CompatibilityOhaeng): CompatibilityOhaengResponse =
            CompatibilityOhaengResponse(ElementResponse.from(ohaeng.element), ohaeng.percentage)
    }
}

/** 오행 표시 정보(코드·독음·한자). */
data class ElementResponse(
    val code: String,
    val label: String,
    val hanja: String,
) {
    companion object {
        fun from(element: CompatibilityElement): ElementResponse = ElementResponse(element.name, element.label, element.hanja)
    }
}
