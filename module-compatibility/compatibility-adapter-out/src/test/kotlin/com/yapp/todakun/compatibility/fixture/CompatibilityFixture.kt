package com.yapp.todakun.compatibility.fixture

import com.yapp.todakun.compatibility.CompatibilityElement
import com.yapp.todakun.compatibility.CompatibilityOhaeng
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.SajuCompatibility
import java.util.UUID

object CompatibilityFixture {
    val COMPATIBILITY_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000e1")
    val MEMBER_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
    val MY_CHART_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000c1")
    val PARTNER_CHART_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000c2")

    fun create(
        id: UUID = COMPATIBILITY_ID,
        memberId: UUID = MEMBER_ID,
        myChartId: UUID = MY_CHART_ID,
        partnerChartId: UUID = PARTNER_CHART_ID,
    ): SajuCompatibility =
        SajuCompatibility.reconstitute(
            id = id,
            memberId = memberId,
            myChartId = myChartId,
            partnerChartId = partnerChartId,
            relationshipType = CompatibilityRelationshipType.LOVER,
            score = 85,
            headline = "함께할수록 빛나는 궁합",
            subheadline = "함께 있을 때, 편안함이 커지는 사이예요.",
            summary = "두 분은 서로의 부족한 기운을 보완하며 평온한 안식처가 되어주는 최상의 흐름을 가지고 있습니다.",
            totalAnalysis = "따뜻한 기운과 유연한 기운이 만나 아름다운 관계를 이룹니다.",
            analysisBasis = "사주 팔자 기반",
            ohaengs = CompatibilityElement.entries.map { CompatibilityOhaeng(it, 20) },
        )
}
