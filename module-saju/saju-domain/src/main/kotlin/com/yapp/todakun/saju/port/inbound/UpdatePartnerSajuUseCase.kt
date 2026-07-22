package com.yapp.todakun.saju.port.inbound

/** 상대방 사주 정보·관계 수정 유스케이스. */
interface UpdatePartnerSajuUseCase {
    fun update(command: UpdatePartnerSajuCommand)
}
