package com.yapp.todakun.saju

/**
 * 한 기둥의 상세(응답 파생 계산 포함). 저장된 [pillar](천간·지지·십성·십이운성)에
 * DB에 저장하지 않는 [jijanggan](지장간)과 [sinsal](십이신살)을 응답 조립 시 덧붙인다.
 */
data class PillarDetail(
    val pillar: SajuPillar,
    val jijanggan: List<HeavenlyStem>,
    val sinsal: Sinsal,
)
