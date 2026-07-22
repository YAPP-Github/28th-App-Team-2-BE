package com.yapp.todakun.saju

import com.yapp.todakun.saju.port.outbound.FourPillars
import com.yapp.todakun.saju.port.outbound.GanjiPillar
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class SajuDetailCalculatorTest : DescribeSpec({
    describe("지장간(hiddenStems)") {
        it("자(子)는 임계다") {
            SajuDetailCalculator.hiddenStems(EarthlyBranch.JA) shouldBe listOf(HeavenlyStem.IM, HeavenlyStem.GYE)
        }

        it("사(巳)는 무경병이다") {
            SajuDetailCalculator.hiddenStems(EarthlyBranch.SA) shouldBe
                listOf(HeavenlyStem.MU, HeavenlyStem.GYEONG, HeavenlyStem.BYEONG)
        }

        it("미(未)는 정을기다") {
            SajuDetailCalculator.hiddenStems(EarthlyBranch.MI) shouldBe
                listOf(HeavenlyStem.JEONG, HeavenlyStem.EUL, HeavenlyStem.GI)
        }

        it("12지지 모두 본기(마지막)의 오행이 지지 오행과 일치한다") {
            EarthlyBranch.entries.forEach { branch ->
                SajuDetailCalculator.hiddenStems(branch).last().element shouldBe branch.element
            }
        }
    }

    describe("십이신살(Sinsal.of) - 巳酉丑 삼합국(겁살=寅) 기준") {
        // 년지 巳 기준: 寅(겁살) 卯(재살) 辰(천살) 巳(지살) 午(년살) 未(월살) 申(망신) 酉(장성) 戌(반안) 亥(역마) 子(육해) 丑(화개)
        it("겁살은 寅이다") {
            Sinsal.of(EarthlyBranch.SA, EarthlyBranch.IN) shouldBe Sinsal.GEOPSAL
        }

        it("왕지(酉)는 장성살이다") {
            Sinsal.of(EarthlyBranch.SA, EarthlyBranch.YU) shouldBe Sinsal.JANGSEONGSAL
        }

        it("고지(丑)는 화개살이다") {
            Sinsal.of(EarthlyBranch.SA, EarthlyBranch.CHUK) shouldBe Sinsal.HWAGAESAL
        }

        it("생지(巳)는 지살이다") {
            Sinsal.of(EarthlyBranch.SA, EarthlyBranch.SA) shouldBe Sinsal.JISAL
        }
    }

    describe("십이신살(Sinsal.of) - 申子辰 삼합국(겁살=巳) 기준") {
        it("겁살은 巳, 왕지(子)는 장성살, 고지(辰)는 화개살이다") {
            Sinsal.of(EarthlyBranch.JA, EarthlyBranch.SA) shouldBe Sinsal.GEOPSAL
            Sinsal.of(EarthlyBranch.JA, EarthlyBranch.JA) shouldBe Sinsal.JANGSEONGSAL
            Sinsal.of(EarthlyBranch.JA, EarthlyBranch.JIN) shouldBe Sinsal.HWAGAESAL
        }
    }

    describe("details(chart) - 년지 기준 신살·지장간 부여") {
        // 년 辛巳 / 월 癸巳 / 일 癸巳 / 시 己未 (일간 癸), 년지 = 巳
        val chart =
            SajuChart.create(
                name = "토닥이",
                gender = Gender.FEMALE,
                calendarType = CalendarType.SOLAR,
                birthDate = java.time.LocalDate.of(2001, 5, 30),
                birthTime = BirthTime.MISI,
                isLeapMonth = false,
                fourPillars =
                    FourPillars(
                        year = GanjiPillar(HeavenlyStem.SIN, EarthlyBranch.SA),
                        month = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
                        day = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
                        hour = GanjiPillar(HeavenlyStem.GI, EarthlyBranch.MI),
                        solarTermName = "입하",
                    ),
            )
        val details = SajuDetailCalculator.details(chart).associateBy { it.pillar.pillarType }

        it("년주(巳)는 지살이다") {
            details.getValue(PillarType.YEAR).sinsal shouldBe Sinsal.JISAL
        }

        it("시주(未)는 월살이다") {
            details.getValue(PillarType.HOUR).sinsal shouldBe Sinsal.WOLSAL
        }

        it("년주 지장간은 무경병이다") {
            details.getValue(PillarType.YEAR).jijanggan shouldBe
                listOf(HeavenlyStem.MU, HeavenlyStem.GYEONG, HeavenlyStem.BYEONG)
        }
    }
})
