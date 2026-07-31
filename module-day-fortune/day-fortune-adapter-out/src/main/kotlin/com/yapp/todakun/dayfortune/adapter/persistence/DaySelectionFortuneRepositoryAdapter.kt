package com.yapp.todakun.dayfortune.adapter.persistence

import com.yapp.todakun.dayfortune.DaySelectionFortune
import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.repository.DaySelectionFortuneRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class DaySelectionFortuneRepositoryAdapter(
    private val daySelectionFortuneJpaRepository: DaySelectionFortuneJpaRepository,
) : DaySelectionFortuneRepository {
    override fun save(daySelectionFortune: DaySelectionFortune): DaySelectionFortune =
        daySelectionFortuneJpaRepository.save(DaySelectionFortuneJpaEntity.fromDomain(daySelectionFortune)).toDomain()

    override fun findByMemberIdAndPurposeAndTargetDate(
        memberId: UUID,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
    ): DaySelectionFortune? =
        daySelectionFortuneJpaRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)?.toDomain()

    override fun lock(
        memberId: UUID,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
    ) = daySelectionFortuneJpaRepository.lock(memberId, purpose.name, targetDate)
}
