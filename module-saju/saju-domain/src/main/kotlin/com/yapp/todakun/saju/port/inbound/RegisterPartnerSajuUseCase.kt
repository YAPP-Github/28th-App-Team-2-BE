package com.yapp.todakun.saju.port.inbound

import java.util.UUID

/** 상대방 사주 등록 유스케이스. 생성된 소유권 링크 ID를 반환한다. */
interface RegisterPartnerSajuUseCase {
    fun register(command: RegisterPartnerSajuCommand): UUID
}
