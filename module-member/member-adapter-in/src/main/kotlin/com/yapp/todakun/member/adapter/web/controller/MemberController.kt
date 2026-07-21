package com.yapp.todakun.member.adapter.web.controller

import com.yapp.todakun.member.adapter.web.MemberApi
import com.yapp.todakun.member.adapter.web.dto.request.UpdateMemberRequest
import com.yapp.todakun.member.adapter.web.dto.request.WithdrawRequest
import com.yapp.todakun.member.adapter.web.dto.response.GetMyProfileResponse
import com.yapp.todakun.member.port.inbound.GetMyProfileUseCase
import com.yapp.todakun.member.port.inbound.UpdateMemberUseCase
import com.yapp.todakun.member.port.inbound.WithdrawMemberUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MemberController(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val updateMemberUseCase: UpdateMemberUseCase,
    private val withdrawMemberUseCase: WithdrawMemberUseCase,
) : MemberApi {
    override fun getMyProfile(memberId: UUID): ResponseEntity<CommonResponse<GetMyProfileResponse>> =
        CommonResponse.retrieved(GetMyProfileResponse.from(getMyProfileUseCase.getProfile(memberId)))

    override fun updateMyProfile(
        memberId: UUID,
        request: UpdateMemberRequest,
    ): ResponseEntity<CommonResponse<Unit>> {
        updateMemberUseCase.update(request.toCommand(memberId))

        return CommonResponse.updated()
    }

    override fun withdraw(
        memberId: UUID,
        accessToken: String,
        request: WithdrawRequest,
    ): ResponseEntity<CommonResponse<Unit>> {
        withdrawMemberUseCase.withdraw(request.toCommand(memberId, accessToken))

        return CommonResponse.deleted()
    }
}
