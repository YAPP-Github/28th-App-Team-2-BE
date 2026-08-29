package com.yapp.todakun.saju.port.inbound

import java.util.UUID

/** 회원이 등록한 상대방 사주 목록 조회 유스케이스(페이징 없이 전체 반환). */
interface GetPartnerSajusUseCase {
    fun getPartners(memberId: UUID): List<PartnerSajuSummary>
}
