package com.yapp.todakun.compatibility.port.inbound

import java.util.UUID

interface GetCompatibilityUseCase {
    fun getById(
        id: UUID,
        memberId: UUID,
    ): CompatibilityResult
}
