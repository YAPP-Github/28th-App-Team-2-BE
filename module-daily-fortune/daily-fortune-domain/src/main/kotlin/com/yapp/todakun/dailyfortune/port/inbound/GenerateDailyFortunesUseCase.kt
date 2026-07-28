package com.yapp.todakun.dailyfortune.port.inbound

import java.time.LocalDate

/**
 * 전체 회원을 대상으로 [fortuneDate] 기준 오늘의 운세를 생성한다(스케줄러가 호출).
 * 회원별 생성은 서로 독립적으로 처리되어, 한 회원의 실패가 나머지 회원의 생성을 막지 않는다.
 */
interface GenerateDailyFortunesUseCase {
    fun generate(fortuneDate: LocalDate)
}
