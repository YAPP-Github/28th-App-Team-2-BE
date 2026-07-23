package com.yapp.todakun.fortune

import com.yapp.todakun.common.validation.validateMaxLength
import com.yapp.todakun.fortune.exception.DailyFortuneContentTooLongException
import com.yapp.todakun.fortune.exception.DailyFortuneItemCountMismatchException
import com.yapp.todakun.fortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.fortune.exception.DailyFortuneTitleTooLongException
import java.time.LocalDate
import java.util.UUID
import kotlin.math.ceil
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private const val TITLE_MAX_LENGTH = 30
private const val CONTENT_MAX_LENGTH = 800
private const val REQUIRED_ITEM_COUNT = 5

/**
 * 매일 회원당 1건 생성되는 오늘의 운세. 통계 활용을 위해 생성 후 내용을 수정하지 않는다(append-only).
 * [score]는 그날 회원의 관심 운세(행운 액션) 점수 평균이다.
 */
data class DailyFortune(
    val id: UUID,
    val memberId: UUID,
    val fortuneDate: LocalDate,
    val score: Int,
    val title: String,
    val content: String,
    val luckyItems: List<String>,
    val cautionaryItems: List<String>,
) {
    /** 다른 회원의 오늘의 운세 존재 여부가 드러나지 않도록 소유자가 다르면 동일하게 404로 처리한다. */
    fun validateOwner(memberId: UUID) {
        if (this.memberId != memberId) {
            throw DailyFortuneNotFoundException()
        }
    }

    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            fortuneDate: LocalDate,
            categoryScores: List<Int>,
            title: String,
            content: String,
            luckyItems: List<String>,
            cautionaryItems: List<String>,
        ): DailyFortune {
            validateLength(title, content)
            validateRequiredItemCounts(luckyItems, cautionaryItems)

            return DailyFortune(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                fortuneDate = fortuneDate,
                score = ceil(categoryScores.average()).toInt(),
                title = title,
                content = content,
                luckyItems = luckyItems,
                cautionaryItems = cautionaryItems,
            )
        }

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            fortuneDate: LocalDate,
            score: Int,
            title: String,
            content: String,
            luckyItems: List<String>,
            cautionaryItems: List<String>,
        ): DailyFortune =
            DailyFortune(
                id = id,
                memberId = memberId,
                fortuneDate = fortuneDate,
                score = score,
                title = title,
                content = content,
                luckyItems = luckyItems,
                cautionaryItems = cautionaryItems,
            )

        private fun validateLength(
            title: String,
            content: String,
        ) {
            validateMaxLength(title, TITLE_MAX_LENGTH) { throw DailyFortuneTitleTooLongException() }
            validateMaxLength(content, CONTENT_MAX_LENGTH) { throw DailyFortuneContentTooLongException() }
        }

        private fun validateRequiredItemCounts(
            luckyItems: List<String>,
            cautionaryItems: List<String>,
        ) {
            if (luckyItems.size != REQUIRED_ITEM_COUNT || cautionaryItems.size != REQUIRED_ITEM_COUNT) {
                throw DailyFortuneItemCountMismatchException()
            }
        }
    }
}
