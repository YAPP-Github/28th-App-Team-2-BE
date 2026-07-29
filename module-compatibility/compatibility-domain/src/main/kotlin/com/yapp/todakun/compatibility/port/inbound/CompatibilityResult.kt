package com.yapp.todakun.compatibility.port.inbound

import com.yapp.todakun.compatibility.CompatibilityOhaeng
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.SajuCompatibility
import java.util.UUID

/**
 * 궁합 생성 결과. 상대 이름([partnerName])은 명식에서 가져와 화면 헤더("○○님과 나의")에 쓴다(궁합 레코드에는 저장하지 않음).
 * 사주원국(4주)은 궁합 응답에 포함하지 않으며, 클라이언트가 saju 상세 API로 별도 조회해 렌더링한다.
 */
data class CompatibilityResult(
    val id: UUID,
    val partnerName: String?,
    val relationshipType: CompatibilityRelationshipType,
    val score: Int,
    val headline: String,
    val subheadline: String,
    val summary: String,
    val totalAnalysis: String,
    val analysisBasis: String,
    val ohaengs: List<CompatibilityOhaeng>,
) {
    companion object {
        fun from(
            compatibility: SajuCompatibility,
            partnerName: String?,
        ): CompatibilityResult =
            CompatibilityResult(
                id = compatibility.id,
                partnerName = partnerName,
                relationshipType = compatibility.relationshipType,
                score = compatibility.score,
                headline = compatibility.headline,
                subheadline = compatibility.subheadline,
                summary = compatibility.summary,
                totalAnalysis = compatibility.totalAnalysis,
                analysisBasis = compatibility.analysisBasis,
                ohaengs = compatibility.ohaengs,
            )
    }
}
