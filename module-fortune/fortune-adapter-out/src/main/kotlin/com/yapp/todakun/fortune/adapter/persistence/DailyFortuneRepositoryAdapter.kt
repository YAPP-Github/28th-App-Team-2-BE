package com.yapp.todakun.fortune.adapter.persistence

import com.yapp.todakun.fortune.DailyFortune
import com.yapp.todakun.fortune.repository.DailyFortuneRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class DailyFortuneRepositoryAdapter(
    private val dailyFortuneJpaRepository: DailyFortuneJpaRepository,
) : DailyFortuneRepository {
    override fun save(dailyFortune: DailyFortune): DailyFortune =
        dailyFortuneJpaRepository.save(DailyFortuneJpaEntity.fromDomain(dailyFortune)).toDomain()

    override fun findById(id: UUID): DailyFortune? = dailyFortuneJpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findByMemberIdAndFortuneDate(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): DailyFortune? = dailyFortuneJpaRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate)?.toDomain()

    override fun findAllByMemberIdBetweenFortuneDates(
        memberId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyFortune> =
        dailyFortuneJpaRepository.findAllByMemberIdAndFortuneDateBetweenOrderByFortuneDateAsc(memberId, from, to).map { it.toDomain() }
}
