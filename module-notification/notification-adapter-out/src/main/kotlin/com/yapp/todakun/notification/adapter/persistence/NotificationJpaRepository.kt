package com.yapp.todakun.notification.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface NotificationJpaRepository : JpaRepository<NotificationJpaEntity, UUID> {
    fun findAllByMemberIdOrderByCreatedAtDesc(memberId: UUID): List<NotificationJpaEntity>

    @Query("select count(n) from NotificationJpaEntity n where n.memberId = :memberId and n.read = false")
    fun countUnread(
        @Param("memberId") memberId: UUID,
    ): Long

    // 같은 트랜잭션에서 방금 저장한(pending) 알림도 반영되도록 사전 flush하고, 이후 조회가 갱신을 보도록 clear한다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update NotificationJpaEntity n set n.read = true where n.memberId = :memberId and n.read = false")
    fun markAllRead(
        @Param("memberId") memberId: UUID,
    ): Int
}
