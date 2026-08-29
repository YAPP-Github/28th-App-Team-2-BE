package com.yapp.todakun.shared

import java.util.UUID

/**
 * 회원의 관리자 권한 조회 확장점. member-adapter-out이 구현한다.
 * auth 도메인이 액세스 토큰 발급 시 role을 클레임에 실을지 분기하기 위해 필요하다.
 */
interface IsAdminMemberPort {
    fun isAdmin(memberId: UUID): Boolean
}
