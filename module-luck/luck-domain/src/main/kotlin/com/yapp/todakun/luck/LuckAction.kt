package com.yapp.todakun.luck

import com.yapp.todakun.shared.FortuneCategory
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

data class LuckAction(
    val id: UUID,
    val memberId: UUID,
    val fortuneCategory: FortuneCategory,
    val score: Int,
    val title: String,
    val content: String,
) {
    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            fortuneCategory: FortuneCategory,
            score: Int,
            title: String,
            content: String,
        ): LuckAction =
            LuckAction(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                fortuneCategory = fortuneCategory,
                score = score,
                title = title,
                content = content,
            )
    }
}
