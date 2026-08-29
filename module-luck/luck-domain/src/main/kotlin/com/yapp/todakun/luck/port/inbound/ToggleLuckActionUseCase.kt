package com.yapp.todakun.luck.port.inbound

import com.yapp.todakun.luck.LuckAction
import java.util.UUID

interface ToggleLuckActionUseCase {
    fun toggle(
        id: UUID,
        memberId: UUID,
    ): LuckAction
}
