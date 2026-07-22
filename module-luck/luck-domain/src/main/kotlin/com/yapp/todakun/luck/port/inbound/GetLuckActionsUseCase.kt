package com.yapp.todakun.luck.port.inbound

import com.yapp.todakun.luck.LuckAction
import java.util.UUID

interface GetLuckActionsUseCase {
    fun getTodayLuckActions(memberId: UUID): List<LuckAction>
}
