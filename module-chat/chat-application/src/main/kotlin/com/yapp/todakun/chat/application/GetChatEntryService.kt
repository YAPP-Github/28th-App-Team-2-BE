package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.port.inbound.ChatEntryResult
import com.yapp.todakun.chat.port.inbound.GetChatEntryUseCase
import com.yapp.todakun.chat.port.outbound.ChatQuotaPort
import com.yapp.todakun.common.annotation.QueryService
import java.util.UUID

@QueryService
class GetChatEntryService(
    private val chatQuotaPort: ChatQuotaPort,
) : GetChatEntryUseCase {
    override fun getEntry(memberId: UUID): ChatEntryResult {
        val quota = chatQuotaPort.getStatus(memberId)

        return ChatEntryResult.of(quota.used, quota.limit)
    }
}
