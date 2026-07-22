package com.yapp.todakun.member.port.inbound

import com.yapp.todakun.member.Member
import java.util.UUID

/** 내 프로필 조회 유스케이스. */
interface GetMyProfileUseCase {
    fun getProfile(memberId: UUID): Member
}
