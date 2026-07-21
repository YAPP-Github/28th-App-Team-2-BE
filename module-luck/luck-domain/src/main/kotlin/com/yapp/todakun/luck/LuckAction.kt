package com.yapp.todakun.luck

import com.yapp.todakun.shared.FortuneCategory
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

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
    fun toggle(): LuckAction = if (achieved) copy(achieved = false) else copy(achieved = true)

    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            fortuneCategory: FortuneCategory,
            fortuneDate: LocalDate,
            score: Int,
            title: String,
            content: String,
        ): LuckAction =
            LuckAction(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                fortuneCategory = fortuneCategory,
                fortuneDate = fortuneDate,
                score = score,
                title = title,
                content = content,
                achieved = false,
            )

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
    }
}
