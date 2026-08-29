package com.yapp.todakun.luck.adapter.web.controller

import com.yapp.todakun.luck.adapter.web.LuckActionApi
import com.yapp.todakun.luck.adapter.web.dto.response.LuckActionListResponse
import com.yapp.todakun.luck.adapter.web.dto.response.LuckActionResponse
import com.yapp.todakun.luck.port.inbound.GetLuckActionUseCase
import com.yapp.todakun.luck.port.inbound.GetLuckActionsUseCase
import com.yapp.todakun.luck.port.inbound.ToggleLuckActionUseCase
import com.yapp.todakun.shared.currentFortuneServiceDate
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class LuckActionController(
    private val getLuckActionUseCase: GetLuckActionUseCase,
    private val getLuckActionsUseCase: GetLuckActionsUseCase,
    private val toggleLuckActionUseCase: ToggleLuckActionUseCase,
) : LuckActionApi {
    override fun getById(
        luckActionId: UUID,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<LuckActionResponse>> {
        val luckAction = getLuckActionUseCase.getById(luckActionId, memberId)

        return CommonResponse.retrieved(LuckActionResponse.from(luckAction))
    }

    override fun getToday(memberId: UUID): ResponseEntity<CommonResponse<List<LuckActionListResponse>>> {
        val luckActions = getLuckActionsUseCase.getLuckActions(memberId, currentFortuneServiceDate())

        return CommonResponse.retrieved(luckActions.map(LuckActionListResponse::from))
    }

    override fun getByDate(
        fortuneDate: LocalDate,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<List<LuckActionListResponse>>> {
        val luckActions = getLuckActionsUseCase.getLuckActions(memberId, fortuneDate)

        return CommonResponse.retrieved(luckActions.map(LuckActionListResponse::from))
    }

    override fun toggle(
        luckActionId: UUID,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<LuckActionResponse>> {
        val toggled = toggleLuckActionUseCase.toggle(luckActionId, memberId)

        return CommonResponse.success(LuckActionResponse.from(toggled))
    }
}
