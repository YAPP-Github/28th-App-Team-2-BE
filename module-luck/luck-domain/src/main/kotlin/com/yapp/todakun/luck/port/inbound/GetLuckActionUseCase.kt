package com.yapp.todakun.luck.port.inbound

import com.yapp.todakun.luck.LuckAction
import java.util.UUID

interface GetLuckActionUseCase {
    fun getById(id: UUID): LuckAction
}
