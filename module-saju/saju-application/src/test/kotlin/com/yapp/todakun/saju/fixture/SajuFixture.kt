package com.yapp.todakun.saju.fixture

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.EarthlyBranch
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.HeavenlyStem
import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.PillarType
import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.SajuCalculator
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.SajuChartSummary
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

    /** 시간 모름은 00:00(자시)로 계산한다 → 癸일간 자시 = 壬子(오서둔). */
    private val jasiHour = GanjiPillar(HeavenlyStem.IM, EarthlyBranch.JA)

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
            fourPillars = fourPillars().let { if (birthTime == BirthTime.UNKNOWN) it.copy(hour = jasiHour) else it },
        )

    /** 시주 없이 저장된 과거 명식(시간 모름을 00시로 계산하기 이전 데이터). 신규 계산으로는 나올 수 없어 DB 복원 형태로 만든다. */
    @ExperimentalUuidApi
    fun chartWithoutHourPillar(): SajuChart =
        chart(birthTime = BirthTime.UNKNOWN).let { chart ->
            val pillars = chart.pillars.filterNot { it.pillarType == PillarType.HOUR }
            chart.copy(
                pillars = pillars,
                ohaeng = SajuCalculator.ohaengDistribution(pillars),
                sipseong = SajuCalculator.sipseongDistribution(pillars),
            )
        }

    fun chartSummary(
        id: UUID = CHART_ID,
        name: String = "토닥이",
        gender: Gender = Gender.FEMALE,
        birthTime: BirthTime = BirthTime.MISI,
    ): SajuChartSummary =
        SajuChartSummary(
            id = id,
            name = name,
            gender = gender,
            calendarType = CalendarType.SOLAR,
            inputDate = LocalDate.of(2001, 5, 30),
            birthTime = birthTime,
            isTimeUnknown = birthTime == BirthTime.UNKNOWN,
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
