package com.yapp.todakun.compatibility

import com.yapp.todakun.compatibility.exception.CompatibilityHeadlineTooLongException
import com.yapp.todakun.compatibility.exception.CompatibilityOhaengElementMismatchException
import com.yapp.todakun.compatibility.exception.CompatibilityScoreOutOfRangeException
import com.yapp.todakun.compatibility.exception.CompatibilitySubheadlineTooLongException
import com.yapp.todakun.compatibility.exception.CompatibilitySummaryTooLongException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class SajuCompatibilityTest : DescribeSpec({
    val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001")
    val myChartId = UUID.fromString("018f0000-0000-7000-8000-0000000000c1")
    val partnerChartId = UUID.fromString("018f0000-0000-7000-8000-0000000000c2")
    val fullOhaengs = CompatibilityElement.entries.map { CompatibilityOhaeng(it, 20) }

    fun create(
        score: Int = 85,
        headline: String = "함께할수록 빛나는 궁합",
        subheadline: String = "함께 있을 때, 편안함이 커지는 사이예요.",
        summary: String = "두 분은 서로의 부족한 기운을 보완하며 평온한 안식처가 되어주는 최상의 흐름을 가지고 있습니다.",
        ohaengs: List<CompatibilityOhaeng> = fullOhaengs,
    ) = SajuCompatibility.create(
        memberId = memberId,
        myChartId = myChartId,
        partnerChartId = partnerChartId,
        relationshipType = CompatibilityRelationshipType.LOVER,
        score = score,
        headline = headline,
        subheadline = subheadline,
        summary = summary,
        totalAnalysis = "따뜻한 정화 기운과 유연한 임수 기운이 만나 아름다운 관계를 이룹니다.",
        ohaengs = ohaengs,
    )

    describe("create") {
        context("정상 입력이면") {
            it("궁합을 생성하고 기본 분석 근거를 채운다") {
                val compatibility = create()

                compatibility.score shouldBe 85
                compatibility.analysisBasis shouldBe "사주 팔자 기반"
                compatibility.ohaengs.size shouldBe 5
            }
        }

        context("점수가 경계값(0, 100)이면") {
            it("정상 생성된다") {
                create(score = 0).score shouldBe 0
                create(score = 100).score shouldBe 100
            }
        }

        context("점수가 0~100 범위를 벗어나면") {
            it("하한 미만(-1)에서 CompatibilityScoreOutOfRangeException을 던진다") {
                shouldThrow<CompatibilityScoreOutOfRangeException> { create(score = -1) }
            }

            it("상한 초과(101)에서 CompatibilityScoreOutOfRangeException을 던진다") {
                shouldThrow<CompatibilityScoreOutOfRangeException> { create(score = 101) }
            }
        }

        context("헤드라인이 최대 길이(50자)를 초과하면") {
            it("CompatibilityHeadlineTooLongException을 던진다") {
                shouldThrow<CompatibilityHeadlineTooLongException> { create(headline = "가".repeat(51)) }
            }
        }

        context("서브헤드라인이 최대 길이(100자)를 초과하면") {
            it("CompatibilitySubheadlineTooLongException을 던진다") {
                shouldThrow<CompatibilitySubheadlineTooLongException> { create(subheadline = "가".repeat(101)) }
            }
        }

        context("요약이 최대 길이(200자)를 초과하면") {
            it("CompatibilitySummaryTooLongException을 던진다") {
                shouldThrow<CompatibilitySummaryTooLongException> { create(summary = "가".repeat(201)) }
            }
        }

        context("오행이 5개가 모두 있지 않으면") {
            it("CompatibilityOhaengElementMismatchException을 던진다") {
                val incomplete = listOf(CompatibilityOhaeng(CompatibilityElement.WOOD, 100))

                shouldThrow<CompatibilityOhaengElementMismatchException> { create(ohaengs = incomplete) }
            }
        }

        context("오행이 6개(중복 포함)로 들어오면") {
            it("CompatibilityOhaengElementMismatchException을 던진다") {
                val withDuplicate = fullOhaengs + CompatibilityOhaeng(CompatibilityElement.WATER, 0)

                shouldThrow<CompatibilityOhaengElementMismatchException> { create(ohaengs = withDuplicate) }
            }
        }
    }
})
