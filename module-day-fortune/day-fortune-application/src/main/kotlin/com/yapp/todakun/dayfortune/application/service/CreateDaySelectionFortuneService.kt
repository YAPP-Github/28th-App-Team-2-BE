package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.port.inbound.CreateDaySelectionFortuneUseCase
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 후보 날짜를 정렬·중복 제거한 뒤 [CreateOneDaySelectionFortuneService]에 날짜별로 위임한다.
 * 정렬은 advisory lock 획득 순서를 고정해, 같은 회원이 겹치는 날짜를 서로 다른 순서로 동시 요청할 때
 * 생길 수 있는 교착을 막는다. 중복 제거는 같은 날짜를 두 번 처리하는 불필요한 락/트랜잭션 왕복을 줄인다.
 */
@Service
class CreateDaySelectionFortuneService(
    private val createOneDaySelectionFortuneService: CreateOneDaySelectionFortuneService,
) : CreateDaySelectionFortuneUseCase {
    @ExperimentalUuidApi
    override fun create(
        purpose: DaySelectionPurpose,
        targetDates: List<LocalDate>,
        memberId: UUID,
    ): List<DaySelectionFortuneResult> =
        targetDates.distinct().sorted().map { targetDate ->
            createOneDaySelectionFortuneService.createOne(purpose, targetDate, memberId)
        }
}
