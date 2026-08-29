package com.yapp.todakun.chat.port.inbound

import java.util.UUID

/** 대화 삭제 유스케이스. 소유자 본인만 삭제할 수 있다. */
interface DeleteConversationUseCase {
    fun delete(
        memberId: UUID,
        conversationId: UUID,
    )
}
