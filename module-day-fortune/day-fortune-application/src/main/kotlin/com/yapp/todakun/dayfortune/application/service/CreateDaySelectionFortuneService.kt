package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.port.inbound.CreateDaySelectionFortuneUseCase
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
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
 *
 * 취소 계약: 한 날짜가 실패하면 `awaitAll()`이 나머지 형제 `Deferred`를 취소한다. [createOne]은 동기(블로킹)
 * 함수이므로 코루틴 취소 신호를 직접 감지하지 못하지만, [runInterruptible]로 감싸 취소 시 실행 스레드에
 * `Thread.interrupt()`가 전달되게 한다 — 아직 시작되지 않은 형제는 즉시 건너뛰고, 실행 중인 형제는 인터럽트에
 * 반응하는 블로킹 I/O(JDBC 소켓 읽기, HTTP 호출 등)라면 그 시점에 중단된다. 다만 이는 최선 노력(best-effort)이며,
 * 인터럽트를 무시하는 호출 구간에서는 취소 이후에도 AI 호출/저장이 끝까지 실행될 수 있다. 이 경우에도
 * [DaySelectionFortuneTransactionalStore.saveIfAbsent]가 날짜별 독립 트랜잭션 + 멱등 저장이므로 데이터
 * 정합성은 깨지지 않는다(부분 완료 허용).
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
                async {
                    runInterruptible { createOneDaySelectionFortuneService.createOne(purpose, targetDate, memberId) }
                }
            }.awaitAll()
        }
}
