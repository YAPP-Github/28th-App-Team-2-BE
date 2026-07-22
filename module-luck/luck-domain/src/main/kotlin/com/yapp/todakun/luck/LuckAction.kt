package com.yapp.todakun.luck

import com.yapp.todakun.luck.exception.LuckActionContentTooLongException
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.exception.LuckActionTitleTooLongException
import com.yapp.todakun.shared.FortuneCategory
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private const val TITLE_MAX_LENGTH = 30
private const val CONTENT_MAX_LENGTH = 200

/**
 * 매일 생성되어 쌓이는 행운 액션 기록. 통계 활용을 위해 (score/title/content 등) 내용은 생성 후 수정하지 않는다(append-only).
 * 다만 [achieved]는 사용자가 실제로 수행했는지 나중에 체크/취소할 수 있는 예외적으로 가변인 필드다.
 */
data class LuckAction(
    val id: UUID,
    val memberId: UUID,
    val fortuneCategory: FortuneCategory,
    val fortuneDate: LocalDate,
    val score: Int,
    val title: String,
    val content: String,
    val achieved: Boolean,
) {
    /** 현재 달성 여부의 반대로 전환한 새 인스턴스를 반환한다(불변). */
    fun toggle(): LuckAction = copy(achieved = !achieved)

    /** 다른 회원의 행운 액션 존재 여부가 드러나지 않도록 소유자가 다르면 동일하게 404로 처리한다. */
    fun validateOwner(memberId: UUID) {
        if (this.memberId != memberId) {
            throw LuckActionNotFoundException()
        }
    }

    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            fortuneCategory: FortuneCategory,
            fortuneDate: LocalDate,
            score: Int,
            title: String,
            content: String,
        ): LuckAction {
            validateLength(title, content)

            return LuckAction(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                fortuneCategory = fortuneCategory,
                fortuneDate = fortuneDate,
                score = score,
                title = title,
                content = content,
                achieved = false,
            )
        }

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            fortuneCategory: FortuneCategory,
            fortuneDate: LocalDate,
            score: Int,
            title: String,
            content: String,
            achieved: Boolean,
        ): LuckAction =
            LuckAction(
                id = id,
                memberId = memberId,
                fortuneCategory = fortuneCategory,
                fortuneDate = fortuneDate,
                score = score,
                title = title,
                content = content,
                achieved = achieved,
            )

        private fun validateLength(
            title: String,
            content: String,
        ) {
            if (title.length > TITLE_MAX_LENGTH) {
                throw LuckActionTitleTooLongException()
            }
            if (content.length > CONTENT_MAX_LENGTH) {
                throw LuckActionContentTooLongException()
            }
        }
    }
}
