package com.yapp.todakun.fortune.adapter.web.controller

import com.yapp.todakun.fortune.adapter.web.FortuneApi
import com.yapp.todakun.fortune.adapter.web.dto.response.FortuneDetailResponse
import com.yapp.todakun.fortune.adapter.web.dto.response.FortuneHistoryResponse
import com.yapp.todakun.fortune.adapter.web.dto.response.TodayFortuneResponse
import com.yapp.todakun.fortune.port.inbound.GetFortuneHistoryUseCase
import com.yapp.todakun.fortune.port.inbound.GetFortuneUseCase
import com.yapp.todakun.fortune.port.inbound.GetTodayFortuneUseCase
import com.yapp.todakun.shared.currentFortuneServiceDate
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class FortuneController(
    private val getTodayFortuneUseCase: GetTodayFortuneUseCase,
    private val getFortuneUseCase: GetFortuneUseCase,
    private val getFortuneHistoryUseCase: GetFortuneHistoryUseCase,
) : FortuneApi {
    override fun getToday(memberId: UUID): ResponseEntity<CommonResponse<TodayFortuneResponse>> {
        val summary = getTodayFortuneUseCase.getToday(memberId, currentFortuneServiceDate())

        return CommonResponse.retrieved(TodayFortuneResponse.from(summary))
    }

    override fun getById(
        fortuneId: UUID,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<FortuneDetailResponse>> {
        val detail = getFortuneUseCase.getById(fortuneId, memberId)

        return CommonResponse.retrieved(FortuneDetailResponse.from(detail))
    }

    override fun getHistory(
        to: LocalDate,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<List<FortuneHistoryResponse>>> {
        val history = getFortuneHistoryUseCase.getHistory(memberId, currentFortuneServiceDate(), to)

        return CommonResponse.retrieved(history.map(FortuneHistoryResponse::from))
    }
}
