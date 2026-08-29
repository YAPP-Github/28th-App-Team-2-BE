package com.yapp.todakun.saju.port.inbound

import java.util.UUID

/** 회원 본인(SELF) 만세력 상세 조회 유스케이스. */
interface GetMySajuUseCase {
    fun getMine(memberId: UUID): SajuChartDetail
}
