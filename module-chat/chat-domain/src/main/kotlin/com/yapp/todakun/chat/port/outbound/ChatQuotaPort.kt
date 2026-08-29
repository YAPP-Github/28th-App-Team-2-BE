package com.yapp.todakun.chat.port.outbound

import java.util.UUID

/**
 * 회원별 하루 무료 채팅 사용량 아웃바운드 포트(자정 기준 리셋). 카운트는 대화 저장과 별개의 저장소(Redis)에 둔다.
 */
interface ChatQuotaPort {
    /** 오늘 사용량을 1 증가시키고 현재 상태를 반환한다. 한도를 이미 소진했으면 [com.yapp.todakun.chat.exception.ChatDailyQuotaExceededException]을 던진다. */
    fun reserve(memberId: UUID): ChatQuotaStatus

    /** 생성 실패 시 방금 예약한 사용량을 되돌린다. */
    fun refund(memberId: UUID)

    fun getStatus(memberId: UUID): ChatQuotaStatus
}

data class ChatQuotaStatus(
    val used: Int,
    val limit: Int,
)
