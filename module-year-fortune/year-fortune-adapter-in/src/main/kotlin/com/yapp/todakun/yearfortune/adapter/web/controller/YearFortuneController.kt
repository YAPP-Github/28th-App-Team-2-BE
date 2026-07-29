package com.yapp.todakun.yearfortune.adapter.web.controller

import com.yapp.todakun.web.response.CommonResponse
import com.yapp.todakun.yearfortune.adapter.web.YearFortuneApi
import com.yapp.todakun.yearfortune.adapter.web.dto.response.YearSelectionFortuneResponse
import com.yapp.todakun.yearfortune.port.inbound.GetYearSelectionFortuneUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class YearFortuneController(
    private val getYearSelectionFortuneUseCase: GetYearSelectionFortuneUseCase,
) : YearFortuneApi {
    override fun getByYear(
        year: Int,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<YearSelectionFortuneResponse>> {
        val detail = getYearSelectionFortuneUseCase.getByYear(year, memberId)

        return CommonResponse.retrieved(YearSelectionFortuneResponse.from(detail))
    }
}
