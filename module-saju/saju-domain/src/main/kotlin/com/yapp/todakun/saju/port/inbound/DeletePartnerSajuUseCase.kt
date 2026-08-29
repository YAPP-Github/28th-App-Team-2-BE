package com.yapp.todakun.saju.port.inbound

import java.util.UUID

/** 상대방 사주 삭제 유스케이스. */
interface DeletePartnerSajuUseCase {
    fun delete(
        memberId: UUID,
        linkId: UUID,
    )
}
