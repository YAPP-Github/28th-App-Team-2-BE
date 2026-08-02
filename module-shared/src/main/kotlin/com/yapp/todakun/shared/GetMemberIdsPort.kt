package com.yapp.todakun.shared

import java.util.UUID

/**
 * 회원 ID를 UUIDv7(시간 정렬) 기준 keyset 페이징으로 조회한다.
 * afterMemberId 이후(오름차순) limit개를 반환하며, null이면 처음부터 조회한다.
 */
interface GetMemberIdsPort {
    fun getMemberIds(
        afterMemberId: UUID?,
        limit: Int,
    ): List<UUID>
}
