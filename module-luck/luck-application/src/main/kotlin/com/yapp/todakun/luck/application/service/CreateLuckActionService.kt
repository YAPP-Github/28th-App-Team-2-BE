package com.yapp.todakun.luck.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.LuckActionPolicy
import com.yapp.todakun.luck.port.inbound.CreateLuckActionCommand
import com.yapp.todakun.luck.port.inbound.CreateLuckActionUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import com.yapp.todakun.shared.CreateLuckActionPort
import com.yapp.todakun.shared.FortuneCategory
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/** 행운 액션 생성 유스케이스. 크로스 도메인 진입점([CreateLuckActionPort])도 함께 구현한다. */
@CommandService
class CreateLuckActionService(
    private val luckActionRepository: LuckActionRepository,
) : CreateLuckActionUseCase, CreateLuckActionPort {
    @ExperimentalUuidApi
    override fun create(command: CreateLuckActionCommand): UUID =
        create(
            memberId = command.memberId,
            fortuneCategory = command.fortuneCategory,
            fortuneDate = command.fortuneDate,
            score = command.score,
            title = command.title,
            content = command.content,
        )

    @ExperimentalUuidApi
    override fun create(
        memberId: UUID,
        fortuneCategory: FortuneCategory,
        fortuneDate: LocalDate,
        score: Int,
        title: String,
        content: String,
    ): UUID {
        LuckActionPolicy.validate(title, content)

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
