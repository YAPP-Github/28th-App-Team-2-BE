package com.yapp.todakun.dailyfortune.port.inbound

import com.yapp.todakun.dailyfortune.DailyFortune
import java.time.LocalDate
import java.util.UUID

/**
 * 회원·날짜 기준 오늘의 운세를 생성한다.
 * 이미 생성되어 있으면 재생성 없이 기존 값을 반환한다(멱등).
 * 조회([GetTodayFortuneUseCase])는 이 유스케이스를 호출하지 않으며, 데이터가 없으면 404(DailyFortuneNotFoundException)를 반환한다.
 */
interface GenerateDailyFortuneUseCase {
    fun generate(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): DailyFortune
}
