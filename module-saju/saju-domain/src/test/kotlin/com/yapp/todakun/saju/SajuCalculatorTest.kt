package com.yapp.todakun.saju

import com.yapp.todakun.saju.port.outbound.FourPillars
import com.yapp.todakun.saju.port.outbound.GanjiPillar
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SajuCalculatorTest : DescribeSpec({
    // 양력 2001-05-30 (라이브러리 검증값): 년 辛巳 / 월 癸巳 / 일 癸巳 / 시 己未 → 일간(일원) 癸
    val fourPillars =
        FourPillars(
            year = GanjiPillar(HeavenlyStem.SIN, EarthlyBranch.SA),
            month = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
            day = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
            hour = GanjiPillar(HeavenlyStem.GI, EarthlyBranch.MI),
            solarTermName = "입하",
        )
    val pillars = SajuCalculator.pillars(fourPillars, HeavenlyStem.GYE)
    val byType = pillars.associateBy { it.pillarType }

    describe("십성 판정 (일간 癸 기준)") {
        it("일주 천간은 일원이라 십성이 없다") {
            byType.getValue(PillarType.DAY).stemSipseong.shouldBeNull()
        }

        it("시주 천간 己(土剋水·음)는 편관") {
            byType.getValue(PillarType.HOUR).stemSipseong shouldBe Sipseong.PYEONGWAN
        }

        it("시주 지지 未(土·음)는 편관") {
            byType.getValue(PillarType.HOUR).branchSipseong shouldBe Sipseong.PYEONGWAN
        }

        it("월주 천간 癸(水·음)는 비견") {
            byType.getValue(PillarType.MONTH).stemSipseong shouldBe Sipseong.BIGYEON
        }

        it("지지 巳(水剋火·양)는 정재") {
            byType.getValue(PillarType.YEAR).branchSipseong shouldBe Sipseong.JEONGJAE
        }
    }

    describe("십이운성 판정 (일간 癸, 음간 역행)") {
        it("지지 未는 묘") {
            byType.getValue(PillarType.HOUR).sibiunseong shouldBe Sibiunseong.MYO
        }

        it("지지 巳는 태") {
            byType.getValue(PillarType.DAY).sibiunseong shouldBe Sibiunseong.TAE
        }
    }

    describe("오행 분포 집계 (8글자)") {
        val ohaeng = SajuCalculator.ohaengDistribution(pillars).associateBy { it.element }

        it("火 3글자(巳巳巳) 37.5%") {
            ohaeng.getValue(Element.FIRE).count shouldBe 3
            ohaeng.getValue(Element.FIRE).percentage shouldBe 37.5
        }

        it("木 0글자") {
            ohaeng.getValue(Element.WOOD).count shouldBe 0
        }

        it("5행 합계는 8글자") {
            ohaeng.values.sumOf { it.count } shouldBe 8
        }
    }

    describe("십성 분포 집계 (일간 제외 7글자)") {
        val sipseong = SajuCalculator.sipseongDistribution(pillars).associateBy { it.sipseong }

        it("정재 3글자(년·월·일 지지 巳巳巳)") {
            sipseong.getValue(Sipseong.JEONGJAE).count shouldBe 3
        }

        it("10종 합계는 7글자") {
            sipseong.values.sumOf { it.count } shouldBe 7
        }
    }
})
