package com.yapp.todakun.luck.repository

import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.shared.FortuneCategory
import java.util.UUID

interface LuckActionRepository {
    fun save(luckAction: LuckAction): LuckAction

    fun findById(id: UUID): LuckAction?

    fun findByMemberIdAndFortuneCategory(
        memberId: UUID,
        fortuneCategory: FortuneCategory,
    ): LuckAction?

    fun findAllByMemberId(memberId: UUID): List<LuckAction>
}
