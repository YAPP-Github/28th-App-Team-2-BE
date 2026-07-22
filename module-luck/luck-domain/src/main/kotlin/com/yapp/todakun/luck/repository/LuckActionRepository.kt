package com.yapp.todakun.luck.repository

import com.yapp.todakun.luck.LuckAction
import java.time.LocalDate
import java.util.UUID

interface LuckActionRepository {
    fun save(luckAction: LuckAction): LuckAction

    fun findById(id: UUID): LuckAction?

    fun findAllByMemberIdAndFortuneDate(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): List<LuckAction>
}
