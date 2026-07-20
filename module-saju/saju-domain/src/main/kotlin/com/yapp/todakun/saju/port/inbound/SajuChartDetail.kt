package com.yapp.todakun.saju.port.inbound

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.HeavenlyStem
import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.OhaengCount
import com.yapp.todakun.saju.PillarDetail
import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.SajuDetailCalculator
import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.SipseongCount
import java.time.LocalDate
import java.util.UUID

/**
 * 만세력 상세 조회 결과. 소유권 링크([MemberSajuLink])와 계산 명식([SajuChart])을 합쳐
 * 화면(사주원국·오행·십성 + 파생 지장간·십이신살)에 필요한 정보를 담는다. 본인·상대 상세에 공통으로 쓰인다.
 */
data class SajuChartDetail(
    val linkId: UUID,
    val role: SajuRole,
    val relationshipType: RelationshipType?,
    val name: String?,
    val gender: Gender,
    val calendarType: CalendarType,
    val birthDate: LocalDate,
    val birthTime: BirthTime,
    val isTimeUnknown: Boolean,
    val dayMaster: HeavenlyStem,
    val pillars: List<PillarDetail>,
    val ohaeng: List<OhaengCount>,
    val sipseong: List<SipseongCount>,
) {
    companion object {
        fun from(
            link: MemberSajuLink,
            chart: SajuChart,
        ): SajuChartDetail =
            SajuChartDetail(
                linkId = link.id,
                role = link.role,
                relationshipType = link.relationshipType,
                name = chart.name,
                gender = chart.gender,
                calendarType = chart.calendarType,
                birthDate = chart.inputDate,
                birthTime = chart.birthTime,
                isTimeUnknown = chart.isTimeUnknown,
                dayMaster = chart.dayMaster,
                pillars = SajuDetailCalculator.details(chart),
                ohaeng = chart.ohaeng,
                sipseong = chart.sipseong,
            )
    }
}
