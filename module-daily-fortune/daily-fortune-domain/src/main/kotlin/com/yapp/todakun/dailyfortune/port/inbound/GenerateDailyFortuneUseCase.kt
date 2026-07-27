package com.yapp.todakun.dailyfortune.port.inbound

import com.yapp.todakun.dailyfortune.DailyFortune
import java.time.LocalDate
import java.util.UUID

/**
 * 회원·날짜 기준 오늘의 운세를 생성한다.
 * 이미 생성되어 있으면 재생성 없이 기존 값을 반환한다(멱등).
 * 신규 온보딩 완료 트리거, 새벽 배치, "오늘의 운세" 조회 시 read-through 폴백이 모두 이 유스케이스 하나로 수렴한다.
 */
interface GenerateDailyFortuneUseCase {
    fun generate(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): DailyFortune
}
