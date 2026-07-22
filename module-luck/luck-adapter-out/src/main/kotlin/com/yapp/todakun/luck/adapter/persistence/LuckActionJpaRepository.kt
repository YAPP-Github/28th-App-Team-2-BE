package com.yapp.todakun.luck.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface LuckActionJpaRepository : JpaRepository<LuckActionJpaEntity, UUID> {
    fun findAllByMemberIdAndFortuneDate(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): List<LuckActionJpaEntity>
}
