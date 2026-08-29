package com.yapp.todakun.notification.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NoticeDispatchHistoryJpaRepository : JpaRepository<NoticeDispatchHistoryJpaEntity, UUID> {
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}
