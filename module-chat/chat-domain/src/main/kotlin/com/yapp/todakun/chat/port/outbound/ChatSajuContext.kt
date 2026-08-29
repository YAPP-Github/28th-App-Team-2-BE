package com.yapp.todakun.chat.port.outbound

/**
 * 토닥이 AI 프롬프트에 넣을 회원 본인(SELF) 명식. saju 도메인 타입을 직접 참조하지 않도록 원시 타입으로만 구성한다.
 * [ohaeng]은 오행 코드별 글자 수, [sipseong]은 십성 라벨별 개수. [hourPillar]는 시주 없이 저장된 과거 명식에서만 null이다(시간 모름은 00:00 기준으로 계산해 채운다).
 */
data class ChatSajuContext(
    val dayMaster: String,
    val yearPillar: ChatPillarContext,
    val monthPillar: ChatPillarContext,
    val dayPillar: ChatPillarContext,
    val hourPillar: ChatPillarContext?,
    val ohaeng: Map<String, Int>,
    val sipseong: Map<String, Int>,
)
