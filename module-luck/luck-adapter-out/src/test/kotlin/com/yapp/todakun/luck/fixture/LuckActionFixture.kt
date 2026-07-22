package com.yapp.todakun.luck.fixture

import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.shared.FortuneCategory
import java.time.LocalDate
import java.util.UUID

private val LUCK_ACTION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private val FORTUNE_DATE: LocalDate = LocalDate.of(2026, 7, 22)
private const val TITLE = "물 마시기"
private const val CONTENT = "하루 8잔의 물을 마셔보세요"

object LuckActionFixture {
    fun create(
        id: UUID = LUCK_ACTION_ID,
        memberId: UUID = MEMBER_ID,
        fortuneCategory: FortuneCategory = FortuneCategory.HEALTH,
        fortuneDate: LocalDate = FORTUNE_DATE,
        score: Int = 80,
        title: String = TITLE,
        content: String = CONTENT,
        achieved: Boolean = false,
    ): LuckAction =
        LuckAction.reconstitute(
            id = id,
            memberId = memberId,
            fortuneCategory = fortuneCategory,
            fortuneDate = fortuneDate,
            score = score,
            title = title,
            content = content,
            achieved = achieved,
        )
}
