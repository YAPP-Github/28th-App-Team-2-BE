package com.yapp.todakun.luck.adapter.persistence

import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.repository.LuckActionRepository
import com.yapp.todakun.shared.CreateLuckActionPort
import com.yapp.todakun.shared.FortuneCategory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@Component
class CreateLuckActionAdapter(
    private val luckActionRepository: LuckActionRepository,
) : CreateLuckActionPort {
    @ExperimentalUuidApi
    override fun create(
        memberId: UUID,
        fortuneCategory: FortuneCategory,
        fortuneDate: LocalDate,
        score: Int,
        title: String,
        content: String,
    ): UUID {
        val luckAction =
            LuckAction.create(
                memberId = memberId,
                fortuneCategory = fortuneCategory,
                fortuneDate = fortuneDate,
                score = score,
                title = title,
                content = content,
            )

        return luckActionRepository.save(luckAction).id
    }
}
