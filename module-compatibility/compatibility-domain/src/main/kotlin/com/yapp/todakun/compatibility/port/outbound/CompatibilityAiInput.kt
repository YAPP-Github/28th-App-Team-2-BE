package com.yapp.todakun.compatibility.port.outbound

import com.yapp.todakun.compatibility.CompatibilityRelationshipType

/** 궁합 AI 생성 입력. 관계 유형과 본인·상대 명식 프로필을 담는다. 개인정보 최소화를 위해 이름은 포함하지 않는다. */
data class CompatibilityAiInput(
    val relationshipType: CompatibilityRelationshipType,
    val myProfile: CompatibilityChartProfile,
    val partnerProfile: CompatibilityChartProfile,
)
