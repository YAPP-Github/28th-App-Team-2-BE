package com.yapp.todakun.member.port.inbound

/** 회원 탈퇴 유스케이스. 사유 로그 저장 후 회원·사주·토큰을 하드 삭제한다. */
interface WithdrawMemberUseCase {
    fun withdraw(command: WithdrawMemberCommand)
}
