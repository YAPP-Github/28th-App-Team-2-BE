package com.yapp.todakun.dayfortune

import com.yapp.todakun.common.validation.validateMaxLength
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneCategoryCountMismatchException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneCategoryDuplicatedException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneContentTooLongException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneDateInPastException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneNotFoundException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneScoreOutOfRangeException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneStarOutOfRangeException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneTitleTooLongException
import java.time.LocalDate
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

/**
 * 회원이 특정 목적으로 선택한 후보 날짜에 대한 운세. 회원당 (목적, 날짜) 조합별로 1건 생성된다.
 */
data class DaySelectionFortune(
    val id: UUID,
    val memberId: UUID,
    val purpose: DaySelectionPurpose,
    val targetDate: LocalDate,
    val score: Int,
    val title: String,
    val content: String,
    val fortuneCategories: List<FortuneCategoryStar>,
) {
    /** 다른 회원의 택일 운세 존재 여부가 드러나지 않도록 소유자가 다르면 동일하게 404로 처리한다. */
    fun validateOwner(memberId: UUID) {
        if (this.memberId != memberId) {
            throw DaySelectionFortuneNotFoundException()
        }
    }

    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            purpose: DaySelectionPurpose,
            targetDate: LocalDate,
            currentDate: LocalDate,
            score: Int,
            title: String,
            content: String,
            fortuneCategories: List<FortuneCategoryStar>,
        ): DaySelectionFortune {
            validateDate(targetDate, currentDate)
            validateMaxLength(title, TITLE_MAX_LENGTH) { throw DaySelectionFortuneTitleTooLongException() }
            validateMaxLength(content, CONTENT_MAX_LENGTH) { throw DaySelectionFortuneContentTooLongException() }
            validateScore(score)
            validateFortuneCategories(fortuneCategories)

            return DaySelectionFortune(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                purpose = purpose,
                targetDate = targetDate,
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
            purpose: DaySelectionPurpose,
            targetDate: LocalDate,
            score: Int,
            title: String,
            content: String,
            fortuneCategories: List<FortuneCategoryStar>,
        ): DaySelectionFortune =
            DaySelectionFortune(
                id = id,
                memberId = memberId,
                purpose = purpose,
                targetDate = targetDate,
                score = score,
                title = title,
                content = content,
                fortuneCategories = fortuneCategories,
            )

        /** 현재 날짜 기준 과거 날짜는 후보로 선택할 수 없다. */
        private fun validateDate(
            targetDate: LocalDate,
            currentDate: LocalDate,
        ) {
            if (targetDate.isBefore(currentDate)) {
                throw DaySelectionFortuneDateInPastException()
            }
        }

        private fun validateScore(score: Int) {
            if (score !in MIN_SCORE..MAX_SCORE) {
                throw DaySelectionFortuneScoreOutOfRangeException()
            }
        }

        private fun validateFortuneCategories(fortuneCategories: List<FortuneCategoryStar>) {
            if (fortuneCategories.size != REQUIRED_CATEGORY_COUNT) {
                throw DaySelectionFortuneCategoryCountMismatchException()
            }
            if (fortuneCategories.map { it.fortuneCategory }.distinct().size != REQUIRED_CATEGORY_COUNT) {
                throw DaySelectionFortuneCategoryDuplicatedException()
            }
            if (fortuneCategories.any { it.star !in MIN_STAR..MAX_STAR }) {
                throw DaySelectionFortuneStarOutOfRangeException()
            }
        }
    }
}
