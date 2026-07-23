package com.yapp.todakun.fortune.port.inbound

import java.util.UUID

interface GetFortuneUseCase {
    fun getById(
        id: UUID,
        memberId: UUID,
    ): FortuneDetail
}
