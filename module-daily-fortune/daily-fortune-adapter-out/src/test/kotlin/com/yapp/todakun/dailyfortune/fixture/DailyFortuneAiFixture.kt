package com.yapp.todakun.dailyfortune.fixture

import com.yapp.todakun.dailyfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
import com.yapp.todakun.dailyfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dailyfortune.port.outbound.Pillar
import com.yapp.todakun.shared.FortuneCategory
import java.time.LocalDate

object DailyFortuneAiFixture {
    fun pillar(
        stem: String,
        branch: String,
        stemSipseong: String? = "비견",
        branchSipseong: String = "정관",
        sibiunseong: String = "장생",
    ): Pillar =
        Pillar(
            stem = stem,
            branch = branch,
            stemSipseong = stemSipseong,
            branchSipseong = branchSipseong,
            sibiunseong = sibiunseong,
        )

    fun memberSajuProfile(): MemberSajuProfile =
        MemberSajuProfile(
            name = "홍길동",
            birthDate = LocalDate.of(1998, 3, 5),
            gender = "MALE",
            job = "WORKER",
            relationshipStatus = "SOLO",
            favoriteFortuneCategories = listOf(FortuneCategory.LOVE, FortuneCategory.MONEY),
            dayMaster = "갑",
            yearPillar = pillar(stem = "갑", branch = "자"),
            monthPillar = pillar(stem = "을", branch = "축"),
            dayPillar = pillar(stem = "갑", branch = "인", stemSipseong = null),
            hourPillar = null,
            ohaeng = mapOf("목" to 3, "화" to 2),
            sipseong = mapOf("비견" to 2, "정관" to 1),
        )

    fun generatedDailyFortune(): GeneratedDailyFortune =
        GeneratedDailyFortune(
            title = "오늘은 새로운 기회가 찾아옵니다",
            content = "오늘의 운세 종합 해석 내용입니다.",
            luckyItems = listOf("파란색", "지갑", "커피", "책", "우산"),
            cautionaryItems = listOf("빨간색", "가위", "동전", "성냥", "칼"),
            categoryFortunes =
                FortuneCategory.entries.map {
                    GeneratedCategoryFortune(fortuneCategory = it, score = 70, title = "오늘의 액션", content = "상세 해석")
                },
        )
}
