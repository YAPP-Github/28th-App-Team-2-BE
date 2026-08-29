package com.yapp.todakun.dailyfortune.port.inbound

import java.time.LocalDate

/**
 * [fortuneDate] 기준 오늘의 운세 생성 배치를 다시 실행한다.
 * 매 호출마다 새 JobInstance로 전체 회원을 다시 훑으므로 이전 실행의 완료 여부와 무관하게 항상 실행 가능하다.
 * 이미 생성된 회원은 [com.yapp.todakun.shared.CreateDailyFortunePort]의 멱등성으로 재생성되지 않고,
 * 누락된 회원만 실제로 생성된다. 같은 날짜로 이미 실행 중인 배치가 있으면 예외가 발생한다.
 */
interface RegenerateDailyFortunesUseCase {
    fun regenerate(fortuneDate: LocalDate)
}
