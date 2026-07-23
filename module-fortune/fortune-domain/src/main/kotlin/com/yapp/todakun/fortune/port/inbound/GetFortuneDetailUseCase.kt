package com.yapp.todakun.fortune.port.inbound

import java.util.UUID

interface GetFortuneDetailUseCase {
    fun getDetail(
        id: UUID,
        memberId: UUID,
    ): FortuneDetail
}
