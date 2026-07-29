package com.yapp.todakun.yearfortune.adapter.persistence

import com.yapp.todakun.yearfortune.YearSelectionFortune
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class YearSelectionFortuneRepositoryAdapter(
    private val yearSelectionFortuneJpaRepository: YearSelectionFortuneJpaRepository,
) : YearSelectionFortuneRepository {
    override fun findByMemberIdAndYear(
        memberId: UUID,
        year: Int,
    ): YearSelectionFortune? = yearSelectionFortuneJpaRepository.findByMemberIdAndYear(memberId, year)?.toDomain()
}
