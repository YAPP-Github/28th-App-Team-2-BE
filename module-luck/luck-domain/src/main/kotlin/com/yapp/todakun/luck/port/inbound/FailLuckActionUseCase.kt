package com.yapp.todakun.luck.port.inbound

import com.yapp.todakun.luck.LuckAction
import java.util.UUID

interface FailLuckActionUseCase {
    fun fail(id: UUID): LuckAction
}
