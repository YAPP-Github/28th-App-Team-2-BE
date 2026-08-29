package com.yapp.todakun.member.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.member.exception.MemberNotFoundException
import com.yapp.todakun.member.port.inbound.UpdateMemberCommand
import com.yapp.todakun.member.port.inbound.UpdateMemberUseCase
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.shared.ReplaceSelfSajuChartPort

/**
 * 내 정보 수정 유스케이스. 회원 정보를 갱신하고, 생년 정보(성별·양음력·생년월일·태어난 시간)가 바뀌면
 * [ReplaceSelfSajuChartPort]로 SELF 사주 명식을 같은 트랜잭션에서 재계산한다.
 */
@CommandService
class UpdateMemberService(
    private val memberRepository: MemberRepository,
    private val replaceSelfSajuChartPort: ReplaceSelfSajuChartPort,
) : UpdateMemberUseCase {
    override fun update(command: UpdateMemberCommand) {
        val member = memberRepository.findById(command.memberId) ?: throw MemberNotFoundException()

        val updated =
            member.update(
                gender = command.gender,
                calendarType = command.calendarType,
                birthDate = command.birthDate,
                birthTime = command.birthTime,
                job = command.job,
                relationshipStatus = command.relationshipStatus,
            )
        memberRepository.save(updated)

        if (!member.sajuInputEquals(updated)) {
            replaceSelfSajuChartPort.replace(
                memberId = updated.id,
                name = updated.name,
                gender = updated.gender.name,
                calendarType = updated.calendarType.name,
                birthDate = updated.birthDate,
                birthTime = updated.birthTime.name,
                isLeapMonth = false,
            )
        }
    }
}
