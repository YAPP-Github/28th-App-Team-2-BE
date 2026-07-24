package com.yapp.todakun.fortune.port.inbound

import java.util.UUID

interface GetDailyFortuneUseCase {
    fun getById(
        id: UUID,
        memberId: UUID,
    ): DailyFortuneDetail
}
