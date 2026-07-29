package com.yapp.todakun.yearfortune.fixture

import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.yearfortune.FortuneCategoryStar
import com.yapp.todakun.yearfortune.YearSelectionFortune
import java.util.UUID

private val YEAR_SELECTION_FORTUNE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000004")
private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private const val TITLE = "새해에는 좋은 기회가 많이 찾아와요"
private const val CONTENT = "전반적으로 안정적인 한 해가 될 것으로 보입니다. 꾸준히 노력한 만큼 결실을 맺을 수 있어요."
private val FORTUNE_CATEGORIES =
    listOf(
        FortuneCategoryStar(FortuneCategory.RELATIONSHIP, 2),
        FortuneCategoryStar(FortuneCategory.MONEY, 3),
        FortuneCategoryStar(FortuneCategory.LOVE, 1),
    )

object YearSelectionFortuneFixture {
    fun create(
        id: UUID = YEAR_SELECTION_FORTUNE_ID,
        memberId: UUID = MEMBER_ID,
        year: Int = 2026,
        score: Int = 80,
        title: String = TITLE,
        content: String = CONTENT,
        fortuneCategories: List<FortuneCategoryStar> = FORTUNE_CATEGORIES,
    ): YearSelectionFortune =
        YearSelectionFortune.reconstitute(
            id = id,
            memberId = memberId,
            year = year,
            score = score,
            title = title,
            content = content,
            fortuneCategories = fortuneCategories,
        )
}
