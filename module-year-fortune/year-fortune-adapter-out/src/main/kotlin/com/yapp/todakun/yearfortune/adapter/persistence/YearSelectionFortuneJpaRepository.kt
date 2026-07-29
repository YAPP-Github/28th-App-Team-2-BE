package com.yapp.todakun.yearfortune.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface YearSelectionFortuneJpaRepository : JpaRepository<YearSelectionFortuneJpaEntity, UUID> {
    fun findByMemberIdAndYear(
        memberId: UUID,
        year: Int,
    ): YearSelectionFortuneJpaEntity?
}
