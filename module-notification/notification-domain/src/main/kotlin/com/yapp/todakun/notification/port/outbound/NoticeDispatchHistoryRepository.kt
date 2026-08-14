package com.yapp.todakun.notification.port.outbound

import com.yapp.todakun.notification.NoticeDispatchHistory

interface NoticeDispatchHistoryRepository {
    /** 아직 처리되지 않은 idempotencyKey일 때만 저장하고 true를 반환한다. 이미 처리된 키면 저장하지 않고 false. */
    fun saveIfAbsent(history: NoticeDispatchHistory): Boolean
}
