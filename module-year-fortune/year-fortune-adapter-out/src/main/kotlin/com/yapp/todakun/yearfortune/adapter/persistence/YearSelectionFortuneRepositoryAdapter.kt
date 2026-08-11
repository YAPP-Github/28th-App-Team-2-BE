package com.yapp.todakun.yearfortune.adapter.persistence

import com.yapp.todakun.yearfortune.YearSelectionFortune
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class YearSelectionFortuneRepositoryAdapter(
    private val yearSelectionFortuneJpaRepository: YearSelectionFortuneJpaRepository,
) : YearSelectionFortuneRepository {
    override fun save(yearSelectionFortune: YearSelectionFortune): YearSelectionFortune =
        yearSelectionFortuneJpaRepository.save(YearSelectionFortuneJpaEntity.fromDomain(yearSelectionFortune)).toDomain()

    override fun findById(id: UUID): YearSelectionFortune? =
        yearSelectionFortuneJpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findByMemberIdAndYear(
        memberId: UUID,
        year: Int,
    ): YearSelectionFortune? = yearSelectionFortuneJpaRepository.findByMemberIdAndYear(memberId, year)?.toDomain()

    override fun findYearsByMemberId(memberId: UUID): List<Int> = yearSelectionFortuneJpaRepository.findYearsByMemberId(memberId)

    override fun lock(
        memberId: UUID,
        year: Int,
    ) = yearSelectionFortuneJpaRepository.lock(memberId, year)
}
