package com.yapp.todakun.luck.port.inbound

import com.yapp.todakun.luck.LuckAction
import java.util.UUID

interface AchieveLuckActionUseCase {
    fun achieve(id: UUID): LuckAction
}
