package com.yapp.todakun.luck.port.inbound

import com.yapp.todakun.luck.LuckAction
import java.time.LocalDate
import java.util.UUID

interface GetLuckActionsUseCase {
    fun getLuckActions(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): List<LuckAction>
}
