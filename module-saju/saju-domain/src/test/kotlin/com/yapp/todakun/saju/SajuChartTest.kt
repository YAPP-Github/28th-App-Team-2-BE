package com.yapp.todakun.saju

import com.yapp.todakun.saju.port.outbound.FourPillars
import com.yapp.todakun.saju.port.outbound.GanjiPillar
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class SajuChartTest : DescribeSpec({
    // 양력 2001-05-30 (미시): 년 辛巳 / 월 癸巳 / 일 癸巳 / 시 己未 (일간 癸)
    val fourPillars =
        FourPillars(
            year = GanjiPillar(HeavenlyStem.SIN, EarthlyBranch.SA),
            month = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
            day = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
            hour = GanjiPillar(HeavenlyStem.GI, EarthlyBranch.MI),
            solarTermName = "입하",
        )

    fun create(birthTime: BirthTime) =
        SajuChart.create(
            name = "토닥이",
            gender = Gender.FEMALE,
            calendarType = CalendarType.SOLAR,
            birthDate = LocalDate.of(2001, 5, 30),
            birthTime = birthTime,
            isLeapMonth = false,
            fourPillars = fourPillars,
        )

    val expectedOrder = listOf(PillarType.YEAR, PillarType.MONTH, PillarType.DAY, PillarType.HOUR)

    describe("create") {
        it("4주를 년→월→일→시 순으로 담는다") {
            create(BirthTime.MISI).pillars.map { it.pillarType } shouldBe expectedOrder
        }

        it("시간 모름도 시주를 포함한다 — 00:00(자시)로 계산하기 때문") {
            create(BirthTime.UNKNOWN).pillars.map { it.pillarType } shouldBe expectedOrder
        }

        it("시간 모름은 입력값을 그대로 보존해 isTimeUnknown이 true다") {
            val chart = create(BirthTime.UNKNOWN)

            chart.birthTime shouldBe BirthTime.UNKNOWN
            chart.isTimeUnknown shouldBe true
        }
    }

    describe("reconstitute") {
        it("어댑터가 뒤섞인 순서로 넘겨도 년→월→일→시로 정규화한다") {
            val chart = create(BirthTime.MISI)

            val restored =
                SajuChart.reconstitute(
                    chart.id,
                    chart.name,
                    chart.gender,
                    chart.calendarType,
                    chart.inputDate,
                    chart.birthTime,
                    chart.isLeapMonth,
                    chart.isTimeUnknown,
                    chart.solarTermName,
                    chart.dayMaster,
                    chart.pillars.reversed(),
                    chart.ohaeng,
                    chart.sipseong,
                )

            restored.pillars.map { it.pillarType } shouldBe expectedOrder
        }
    }
})
