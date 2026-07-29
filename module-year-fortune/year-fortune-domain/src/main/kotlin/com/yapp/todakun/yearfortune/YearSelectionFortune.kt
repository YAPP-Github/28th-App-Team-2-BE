package com.yapp.todakun.yearfortune

import com.yapp.todakun.common.validation.validateMaxLength
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneCategoryCountMismatchException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneCategoryDuplicatedException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneContentTooLongException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneNotFoundException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneScoreOutOfRangeException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneStarOutOfRangeException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneTitleTooLongException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneYearOutOfRangeException
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private const val TITLE_MAX_LENGTH = 30
private const val CONTENT_MAX_LENGTH = 500
private const val REQUIRED_CATEGORY_COUNT = 3
private const val MIN_STAR = 1
private const val MAX_STAR = 3
private const val MIN_SCORE = 0
private const val MAX_SCORE = 100
private const val YEAR_RANGE_OFFSET = 5

/**
 * 회원이 선택한 연도에 대한 운세. 회원당 연도별로 1건 생성된다.
 */
data class YearSelectionFortune(
    val id: UUID,
    val memberId: UUID,
    val year: Int,
    val score: Int,
    val title: String,
    val content: String,
    val fortuneCategories: List<FortuneCategoryStar>,
) {
    /** 다른 회원의 연도 선택 운세 존재 여부가 드러나지 않도록 소유자가 다르면 동일하게 404로 처리한다. */
    fun validateOwner(memberId: UUID) {
        if (this.memberId != memberId) {
            throw YearSelectionFortuneNotFoundException()
        }
    }

    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            year: Int,
            currentYear: Int,
            score: Int,
            title: String,
            content: String,
            fortuneCategories: List<FortuneCategoryStar>,
        ): YearSelectionFortune {
            validateYear(year, currentYear)
            validateMaxLength(title, TITLE_MAX_LENGTH) { throw YearSelectionFortuneTitleTooLongException() }
            validateMaxLength(content, CONTENT_MAX_LENGTH) { throw YearSelectionFortuneContentTooLongException() }
            validateScore(score)
            validateFortuneCategories(fortuneCategories)

            return YearSelectionFortune(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                year = year,
                score = score,
                title = title,
                content = content,
                fortuneCategories = fortuneCategories,
            )
        }

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            year: Int,
            score: Int,
            title: String,
            content: String,
            fortuneCategories: List<FortuneCategoryStar>,
        ): YearSelectionFortune =
            YearSelectionFortune(
                id = id,
                memberId = memberId,
                year = year,
                score = score,
                title = title,
                content = content,
                fortuneCategories = fortuneCategories,
            )

        /** 조회 시점에도 조회 가능 연도 범위(±5년)를 검사할 수 있도록 공개한다. */
        private fun validateYear(
            year: Int,
            currentYear: Int,
        ) {
            if (year !in (currentYear - YEAR_RANGE_OFFSET)..(currentYear + YEAR_RANGE_OFFSET)) {
                throw YearSelectionFortuneYearOutOfRangeException()
            }
        }

        private fun validateScore(score: Int) {
            if (score !in MIN_SCORE..MAX_SCORE) {
                throw YearSelectionFortuneScoreOutOfRangeException()
            }
        }

        private fun validateFortuneCategories(fortuneCategories: List<FortuneCategoryStar>) {
            if (fortuneCategories.size != REQUIRED_CATEGORY_COUNT) {
                throw YearSelectionFortuneCategoryCountMismatchException()
            }
            if (fortuneCategories.map { it.fortuneCategory }.distinct().size != REQUIRED_CATEGORY_COUNT) {
                throw YearSelectionFortuneCategoryDuplicatedException()
            }
            if (fortuneCategories.any { it.star !in MIN_STAR..MAX_STAR }) {
                throw YearSelectionFortuneStarOutOfRangeException()
            }
        }
    }
}
