package com.yapp.todakun.saju.fixture

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.EarthlyBranch
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.HeavenlyStem
import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.port.outbound.FourPillars
import com.yapp.todakun.saju.port.outbound.GanjiPillar
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

object SajuFixture {
    val MEMBER_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
    val CHART_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000c1")
    val LINK_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-0000000000d1")

    /** 양력 2001-05-30 검증값: 년 辛巳 / 월 癸巳 / 일 癸巳 / 시 己未 (일간 癸). */
    fun fourPillars(): FourPillars =
        FourPillars(
            year = GanjiPillar(HeavenlyStem.SIN, EarthlyBranch.SA),
            month = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
            day = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
            hour = GanjiPillar(HeavenlyStem.GI, EarthlyBranch.MI),
            solarTermName = "입하",
        )

    @ExperimentalUuidApi
    fun chart(
        name: String = "토닥이",
        gender: Gender = Gender.FEMALE,
        birthTime: BirthTime = BirthTime.MISI,
    ): SajuChart =
        SajuChart.create(
            name = name,
            gender = gender,
            calendarType = CalendarType.SOLAR,
            birthDate = LocalDate.of(2001, 5, 30),
            birthTime = birthTime,
            isLeapMonth = false,
            fourPillars = fourPillars().let { if (birthTime == BirthTime.UNKNOWN) it.copy(hour = null) else it },
        )

    fun selfLink(
        id: UUID = LINK_ID,
        memberId: UUID = MEMBER_ID,
        chartId: UUID = CHART_ID,
    ): MemberSajuLink = MemberSajuLink.reconstitute(id, memberId, chartId, SajuRole.SELF, null)

    fun partnerLink(
        id: UUID = LINK_ID,
        memberId: UUID = MEMBER_ID,
        chartId: UUID = CHART_ID,
        relationshipType: RelationshipType = RelationshipType.LOVER,
    ): MemberSajuLink = MemberSajuLink.reconstitute(id, memberId, chartId, SajuRole.PARTNER, relationshipType)
}
