package com.yapp.todakun.dailyfortune.adapter.web.controller

import com.yapp.todakun.dailyfortune.adapter.web.DailyFortuneApi
import com.yapp.todakun.dailyfortune.adapter.web.dto.response.DailyFortuneHistoryResponse
import com.yapp.todakun.dailyfortune.adapter.web.dto.response.DailyFortuneResponse
import com.yapp.todakun.dailyfortune.adapter.web.dto.response.TodayFortuneResponse
import com.yapp.todakun.dailyfortune.port.inbound.GetDailyFortuneHistoryUseCase
import com.yapp.todakun.dailyfortune.port.inbound.GetDailyFortuneUseCase
import com.yapp.todakun.dailyfortune.port.inbound.GetTodayFortuneUseCase
import com.yapp.todakun.dailyfortune.port.inbound.RestartDailyFortunesUseCase
import com.yapp.todakun.shared.currentFortuneServiceDate
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class DailyFortuneController(
    private val getTodayFortuneUseCase: GetTodayFortuneUseCase,
    private val getFortuneUseCase: GetDailyFortuneUseCase,
    private val getFortuneHistoryUseCase: GetDailyFortuneHistoryUseCase,
    private val restartDailyFortunesUseCase: RestartDailyFortunesUseCase,
) : DailyFortuneApi {
    override fun getToday(memberId: UUID): ResponseEntity<CommonResponse<TodayFortuneResponse>> {
        val summary = getTodayFortuneUseCase.getToday(memberId, currentFortuneServiceDate())

        return CommonResponse.retrieved(TodayFortuneResponse.from(summary))
    }

    override fun getById(
        dailyFortuneId: UUID,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<DailyFortuneResponse>> {
        val detail = getFortuneUseCase.getById(dailyFortuneId, memberId)

        return CommonResponse.retrieved(DailyFortuneResponse.from(detail))
    }

    override fun getHistory(
        to: LocalDate,
        memberId: UUID,
    ): ResponseEntity<CommonResponse<List<DailyFortuneHistoryResponse>>> {
        val history = getFortuneHistoryUseCase.getHistory(memberId, currentFortuneServiceDate(), to)

        return CommonResponse.retrieved(history.map(DailyFortuneHistoryResponse::from))
    }

    override fun restart(fortuneDate: LocalDate): ResponseEntity<CommonResponse<Unit>> {
        restartDailyFortunesUseCase.restart(fortuneDate)

        return CommonResponse.success(Unit)
    }
}
