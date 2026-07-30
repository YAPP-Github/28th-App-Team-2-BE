package com.yapp.todakun.dayfortune.fixture

import com.yapp.todakun.dayfortune.DaySelectionFortune
import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.FortuneCategoryStar
import com.yapp.todakun.shared.FortuneCategory
import java.time.LocalDate
import java.util.UUID

private val DAY_SELECTION_FORTUNE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000004")
private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private const val TITLE = "이 날은 새로운 시작에 좋은 기운이 감돌아요"
private const val CONTENT = "전반적으로 안정적인 기운이 흐르는 날입니다. 꾸준히 준비한 만큼 좋은 결과를 기대할 수 있어요."
private val FORTUNE_CATEGORIES =
    listOf(
        FortuneCategoryStar(FortuneCategory.RELATIONSHIP, 2),
        FortuneCategoryStar(FortuneCategory.MONEY, 3),
        FortuneCategoryStar(FortuneCategory.LOVE, 1),
    )

object DaySelectionFortuneFixture {
    fun create(
        id: UUID = DAY_SELECTION_FORTUNE_ID,
        memberId: UUID = MEMBER_ID,
        purpose: DaySelectionPurpose = DaySelectionPurpose.TRAVEL,
        targetDate: LocalDate = LocalDate.of(2026, 8, 1),
        score: Int = 80,
        title: String = TITLE,
        content: String = CONTENT,
        fortuneCategories: List<FortuneCategoryStar> = FORTUNE_CATEGORIES,
    ): DaySelectionFortune =
        DaySelectionFortune.reconstitute(
            id = id,
            memberId = memberId,
            purpose = purpose,
            targetDate = targetDate,
            score = score,
            title = title,
            content = content,
            fortuneCategories = fortuneCategories,
        )
}
