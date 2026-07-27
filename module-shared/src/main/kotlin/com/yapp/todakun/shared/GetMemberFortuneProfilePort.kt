package com.yapp.todakun.shared

import java.util.UUID

/**
 * 오늘의 운세(daily-fortune 도메인)가 AI 생성 입력으로 쓸 회원 정보를 조회하는 확장점.
 * "오늘의 운세" 도메인이 member 도메인과 직접 결합하지 않도록 이 포트를 거친다.
 */
interface GetMemberFortuneProfilePort {
    fun getProfile(memberId: UUID): MemberFortuneProfile
}
