package com.yapp.todakun.fortune.fixture

import com.yapp.todakun.fortune.DailyFortune
import java.time.LocalDate
import java.util.UUID

private val DAILY_FORTUNE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000003")
private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private const val TITLE = "오늘은 활기찬 하루가 될 거예요"
private const val CONTENT = "전반적으로 운이 상승하는 하루입니다. 자신감을 갖고 하루를 시작해보세요."

object DailyFortuneFixture {
    fun create(
        id: UUID = DAILY_FORTUNE_ID,
        memberId: UUID = MEMBER_ID,
        fortuneDate: LocalDate = LocalDate.of(2026, 6, 24),
        score: Int = 80,
        title: String = TITLE,
        content: String = CONTENT,
        luckyItems: List<String> = listOf("노란색", "마스크", "운동화", "셔츠", "안경"),
        cautionaryItems: List<String> = listOf("검정색", "체크무늬", "라면", "시계", "우산"),
    ): DailyFortune =
        DailyFortune.reconstitute(
            id = id,
            memberId = memberId,
            fortuneDate = fortuneDate,
            score = score,
            title = title,
            content = content,
            luckyItems = luckyItems,
            cautionaryItems = cautionaryItems,
        )
}
