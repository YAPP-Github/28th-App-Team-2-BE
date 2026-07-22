package com.yapp.todakun.member.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.member.Member
import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.port.inbound.GetMyProfileUseCase
import com.yapp.todakun.member.repository.MemberRepository
import java.util.UUID

/** 내 프로필 조회 유스케이스. */
@QueryService
class GetMyProfileService(
    private val memberRepository: MemberRepository,
) : GetMyProfileUseCase {
    override fun getProfile(memberId: UUID): Member = memberRepository.findById(memberId) ?: throw MemberNotFoundException()
}
