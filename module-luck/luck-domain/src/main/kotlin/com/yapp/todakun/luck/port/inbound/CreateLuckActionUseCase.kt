package com.yapp.todakun.luck.port.inbound

import com.yapp.todakun.shared.FortuneCategory
import java.util.UUID

interface CreateLuckActionUseCase {
    fun create(command: CreateLuckActionCommand): UUID
}

data class CreateLuckActionCommand(
    val memberId: UUID,
    val fortuneCategory: FortuneCategory,
    val score: Int,
    val title: String,
    val content: String,
)
