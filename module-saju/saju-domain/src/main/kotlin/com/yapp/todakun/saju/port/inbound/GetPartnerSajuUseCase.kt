package com.yapp.todakun.saju.port.inbound

import java.util.UUID

/** 상대방 명식 상세 조회 유스케이스. */
interface GetPartnerSajuUseCase {
    fun getPartner(
        memberId: UUID,
        linkId: UUID,
    ): SajuChartDetail
}
