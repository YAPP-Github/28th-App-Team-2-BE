package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.exception.PartnerSajuLimitExceededException
import com.yapp.todakun.saju.port.inbound.RegisterPartnerSajuCommand
import com.yapp.todakun.saju.port.inbound.RegisterPartnerSajuUseCase
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/** 최대 등록 가능한 상대방 사주 수. */
private const val MAX_PARTNER_COUNT = 10L

/** 상대방 사주 등록 유스케이스. 최대 10명 제한을 검증하고 명식 계산·저장 후 소유권 링크를 만든다. */
@CommandService
class RegisterPartnerSajuService(
    private val manseryeokPort: ManseryeokPort,
    private val sajuChartRepository: SajuChartRepository,
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
) : RegisterPartnerSajuUseCase {
    @ExperimentalUuidApi
    override fun register(command: RegisterPartnerSajuCommand): UUID {
        if (memberSajuLinkRepository.countPartnersByMemberId(command.memberId) >= MAX_PARTNER_COUNT) {
            throw PartnerSajuLimitExceededException()
        }

        val fourPillars =
            manseryeokPort.calculate(
                birthDate = command.birthDate,
                birthTime = command.birthTime.toSajuEnum<BirthTime>(),
                calendarType = command.calendarType.toSajuEnum<CalendarType>(),
                isLeapMonth = false,
            )

        val chartId =
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

        return memberSajuLinkRepository
            .save(
                MemberSajuLink.partner(
                    memberId = command.memberId,
                    chartId = chartId,
                    relationshipType = command.relationshipType.toSajuEnum<RelationshipType>(),
                ),
            ).id
    }
}
