package com.yapp.todakun.dayfortune.adapter.web.controller

import com.yapp.todakun.dayfortune.adapter.web.DayFortuneApi
import com.yapp.todakun.dayfortune.adapter.web.dto.request.CreateDaySelectionFortuneRequest
import com.yapp.todakun.dayfortune.adapter.web.dto.response.DaySelectionFortuneResponse
import com.yapp.todakun.dayfortune.port.inbound.CreateDaySelectionFortuneUseCase
import com.yapp.todakun.dayfortune.port.inbound.GetDaySelectionFortuneUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DayFortuneController(
    private val createDaySelectionFortuneUseCase: CreateDaySelectionFortuneUseCase,
    private val getDaySelectionFortuneUseCase: GetDaySelectionFortuneUseCase,
) : DayFortuneApi {
    override fun create(
        request: CreateDaySelectionFortuneRequest,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<List<DaySelectionFortuneResponse>>> {
        val results =
            createDaySelectionFortuneUseCase.create(
                purpose = request.toPurpose(),
                targetDates = request.targetDates,
                memberId = memberId,
            )

        return CommonResponse.created(results.map(DaySelectionFortuneResponse::from))
    }

    override fun getById(
        dayFortuneId: UUID,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<DaySelectionFortuneResponse>> {
        val result = getDaySelectionFortuneUseCase.getById(dayFortuneId, memberId)

        return CommonResponse.retrieved(DaySelectionFortuneResponse.from(result))
    }
}
