package com.yapp.todakun.luck.adapter.persistence

import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.repository.LuckActionRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
class LuckActionRepositoryAdapter(
    private val luckActionJpaRepository: LuckActionJpaRepository,
) : LuckActionRepository {
    override fun save(luckAction: LuckAction): LuckAction =
        luckActionJpaRepository.save(LuckActionJpaEntity.fromDomain(luckAction)).toDomain()

    override fun findById(id: UUID): LuckAction? = luckActionJpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findAllByMemberIdAndFortuneDate(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): List<LuckAction> = luckActionJpaRepository.findAllByMemberIdAndFortuneDate(memberId, fortuneDate).map { it.toDomain() }
}
