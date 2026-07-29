package com.yapp.todakun.shared

import java.util.UUID

/**
 * 오늘의 운세·연도별 운세(daily-fortune·year-fortune 도메인)가 AI 생성 입력으로 쓸 회원 본인(SELF) 사주 명식을 조회하는 확장점.
 * 각 운세 도메인이 saju 도메인과 직접 결합하지 않도록 이 포트를 거친다.
 */
interface GetSajuChartPort {
    fun getChart(memberId: UUID): SajuChartSummary
}
