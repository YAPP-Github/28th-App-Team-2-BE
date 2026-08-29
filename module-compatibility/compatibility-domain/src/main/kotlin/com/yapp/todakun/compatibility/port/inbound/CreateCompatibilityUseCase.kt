package com.yapp.todakun.compatibility.port.inbound

import java.util.UUID

interface CreateCompatibilityUseCase {
    fun create(
        memberId: UUID,
        partnerLinkId: UUID,
    ): CompatibilityResult
}
