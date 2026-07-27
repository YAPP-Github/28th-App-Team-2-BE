package com.yapp.todakun.shared

import java.time.LocalDate

/**
 * 오늘의 운세(daily-fortune 도메인)가 AI 생성 입력으로 쓸 특정 날짜의 일진(당일 간지)을 조회하는 확장점.
 * 회원 개인 정보와 무관한 달력 계산이라 memberId를 받지 않는다.
 */
interface GetDailyPillarPort {
    fun getPillar(fortuneDate: LocalDate): PillarSummary
}
