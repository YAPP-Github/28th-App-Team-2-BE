package com.yapp.todakun.member.port.inbound

/** 내 정보 수정 유스케이스. */
interface UpdateMemberUseCase {
    fun update(command: UpdateMemberCommand)
}
