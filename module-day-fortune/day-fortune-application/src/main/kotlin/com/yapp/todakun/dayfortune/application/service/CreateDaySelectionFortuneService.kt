package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.port.inbound.CreateDaySelectionFortuneUseCase
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 후보 날짜를 정렬·중복 제거한 뒤 [CreateOneDaySelectionFortuneService]에 날짜별로 위임한다.
 * 날짜별 위임은 코루틴(`async`/`awaitAll`)으로 병렬 실행한다.
 * 지배적인 지연 원인이 날짜당 순차 AI 호출이기 때문이다.
 * [CreateOneDaySelectionFortuneService.createOne]의 lock 조회/저장은 각각 짧은 트랜잭션에서 advisory lock을
 * 정확히 1개만 잡고 커밋과 함께 해제하므로, 한 트랜잭션이 동시에 2개 락을 보유하는 경우가 구조적으로 없어
 * 날짜별 병렬 실행이 순환 대기(교착)를 일으키지 않는다.
 * 정렬은 이제 락 순서 고정 목적이 아니라 반환 결과의 순서를 날짜순으로 결정적으로 만들기 위함이고,
 * 중복 제거는 같은 날짜를 두 번 처리하는 불필요한 락/트랜잭션 왕복 및 AI 호출을 줄인다.
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
        runBlocking(Dispatchers.IO) {
            targetDates.distinct().sorted().map { targetDate ->
                async { createOneDaySelectionFortuneService.createOne(purpose, targetDate, memberId) }
            }.awaitAll()
        }
}
