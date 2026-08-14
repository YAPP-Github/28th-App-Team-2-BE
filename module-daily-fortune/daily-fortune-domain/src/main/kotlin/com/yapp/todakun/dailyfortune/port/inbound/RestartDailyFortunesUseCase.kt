package com.yapp.todakun.dailyfortune.port.inbound

import java.time.LocalDate

/**
 * [fortuneDate] 기준 오늘의 운세 생성 배치의 마지막 실행을 재시도한다.
 * 마지막으로 커밋된 회원 다음부터 이어서 처리되며(Spring Batch의 재시작 특성), 이미 생성된 회원은 다시 생성하지 않는다.
 */
interface RestartDailyFortunesUseCase {
    fun restart(fortuneDate: LocalDate)
}
