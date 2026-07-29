package com.yapp.todakun.yearfortune.adapter.web.controller

import com.yapp.todakun.web.response.CommonResponse
import com.yapp.todakun.yearfortune.adapter.web.YearFortuneApi
import com.yapp.todakun.yearfortune.adapter.web.dto.response.YearSelectionFortuneResponse
import com.yapp.todakun.yearfortune.port.inbound.CreateYearSelectionFortuneUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class YearFortuneController(
    private val createYearSelectionFortuneUseCase: CreateYearSelectionFortuneUseCase,
) : YearFortuneApi {
    override fun create(
        year: Int,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<YearSelectionFortuneResponse>> {
        val result = createYearSelectionFortuneUseCase.create(year, memberId)

        return CommonResponse.created(YearSelectionFortuneResponse.from(result))
    }
}
