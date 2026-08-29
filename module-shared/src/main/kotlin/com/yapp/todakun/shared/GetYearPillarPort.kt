package com.yapp.todakun.shared

/**
 * 연도별 운세(year-fortune 도메인)가 AI 생성 입력으로 쓸 특정 연도의 세운(연주)을 조회하는 확장점.
 * 회원 개인 정보와 무관한 달력 계산이라 memberId를 받지 않는다.
 */
interface GetYearPillarPort {
    fun getPillar(year: Int): PillarSummary
}
