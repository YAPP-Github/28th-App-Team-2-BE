package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.port.inbound.UpdatePartnerSajuCommand
import com.yapp.todakun.saju.port.inbound.UpdatePartnerSajuUseCase
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import kotlin.uuid.ExperimentalUuidApi

/** 상대방 사주 수정 유스케이스. 소유권 검증 후 명식을 재계산해 교체하고 관계 라벨을 갱신한다. */
@CommandService
class UpdatePartnerSajuService(
    private val manseryeokPort: ManseryeokPort,
    private val sajuChartRepository: SajuChartRepository,
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
) : UpdatePartnerSajuUseCase {
    @ExperimentalUuidApi
    override fun update(command: UpdatePartnerSajuCommand) {
        val link =
            memberSajuLinkRepository.findByIdAndMemberId(command.linkId, command.memberId)
                ?.takeIf { it.role == SajuRole.PARTNER }
                ?: throw SajuChartNotFoundException()

        val fourPillars =
            manseryeokPort.calculate(
                birthDate = command.birthDate,
                birthTime = command.birthTime.toSajuEnum<BirthTime>(),
                calendarType = command.calendarType.toSajuEnum<CalendarType>(),
                isLeapMonth = false,
            )

        val newChartId =
            sajuChartRepository
                .save(
                    SajuChart.create(
                        name = command.name,
                        gender = command.gender.toSajuEnum<Gender>(),
                        calendarType = command.calendarType.toSajuEnum<CalendarType>(),
                        birthDate = command.birthDate,
                        birthTime = command.birthTime.toSajuEnum<BirthTime>(),
                        isLeapMonth = false,
                        fourPillars = fourPillars,
                    ),
                ).id

        sajuChartRepository.deleteById(link.chartId)
        memberSajuLinkRepository.save(
            link.updated(chartId = newChartId, relationshipType = command.relationshipType.toSajuEnum<RelationshipType>()),
        )
    }
}
