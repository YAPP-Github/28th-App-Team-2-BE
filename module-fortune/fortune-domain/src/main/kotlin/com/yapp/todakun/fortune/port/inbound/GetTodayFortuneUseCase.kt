package com.yapp.todakun.fortune.port.inbound

import java.time.LocalDate
import java.util.UUID

interface GetTodayFortuneUseCase {
    /** [fortuneDate]에 해당하는 오늘의 운세가 아직 생성되지 않았으면(예: 가입 직후) null을 반환한다. */
    fun getToday(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): TodayFortuneSummary?
}
