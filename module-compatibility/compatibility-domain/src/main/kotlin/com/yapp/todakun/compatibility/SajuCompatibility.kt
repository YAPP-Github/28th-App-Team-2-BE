package com.yapp.todakun.compatibility

import com.yapp.todakun.common.validation.validateMaxLength
import com.yapp.todakun.compatibility.exception.CompatibilityHeadlineTooLongException
import com.yapp.todakun.compatibility.exception.CompatibilityOhaengElementMismatchException
import com.yapp.todakun.compatibility.exception.CompatibilityScoreOutOfRangeException
import com.yapp.todakun.compatibility.exception.CompatibilitySubheadlineTooLongException
import com.yapp.todakun.compatibility.exception.CompatibilitySummaryTooLongException
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private const val HEADLINE_MAX_LENGTH = 50
private const val SUBHEADLINE_MAX_LENGTH = 100
private const val SUMMARY_MAX_LENGTH = 200
private const val MIN_SCORE = 0
private const val MAX_SCORE = 100
private const val OHAENG_ELEMENT_COUNT = 5
private const val DEFAULT_ANALYSIS_BASIS = "사주 팔자 기반"

/**
 * 두 사주 명식(본인·상대)을 조합한 궁합 결과 애그리거트. 회원당 (본인 명식, 상대 명식) 조합으로 1건 생성된다(멱등).
 * 점수·헤드라인·서브헤드라인·요약·총운은 AI 생성 결과이고, [ohaengs]는 두 명식 오행을 합산·정규화한 결정적 계산 결과(합계 100)다.
 * [relationshipType]은 비교 시점 관계 유형 스냅샷, [analysisBasis]는 분석 근거 표기 문구.
 */
@ConsistentCopyVisibility
data class SajuCompatibility private constructor(
    val id: UUID,
    val memberId: UUID,
    val myChartId: UUID,
    val partnerChartId: UUID,
    val relationshipType: CompatibilityRelationshipType,
    val score: Int,
    val headline: String,
    val subheadline: String,
    val summary: String,
    val totalAnalysis: String,
    val analysisBasis: String,
    val ohaengs: List<CompatibilityOhaeng>,
) {
    companion object {
        @ExperimentalUuidApi
        @Suppress("LongParameterList")
        fun create(
            memberId: UUID,
            myChartId: UUID,
            partnerChartId: UUID,
            relationshipType: CompatibilityRelationshipType,
            score: Int,
            headline: String,
            subheadline: String,
            summary: String,
            totalAnalysis: String,
            ohaengs: List<CompatibilityOhaeng>,
            analysisBasis: String = DEFAULT_ANALYSIS_BASIS,
        ): SajuCompatibility {
            validateScore(score)
            validateMaxLength(headline, HEADLINE_MAX_LENGTH) { throw CompatibilityHeadlineTooLongException() }
            validateMaxLength(subheadline, SUBHEADLINE_MAX_LENGTH) { throw CompatibilitySubheadlineTooLongException() }
            validateMaxLength(summary, SUMMARY_MAX_LENGTH) { throw CompatibilitySummaryTooLongException() }
            validateOhaengs(ohaengs)

            return SajuCompatibility(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                myChartId = myChartId,
                partnerChartId = partnerChartId,
                relationshipType = relationshipType,
                score = score,
                headline = headline,
                subheadline = subheadline,
                summary = summary,
                totalAnalysis = totalAnalysis,
                analysisBasis = analysisBasis,
                ohaengs = ohaengs,
            )
        }

        @JvmStatic
        @Suppress("LongParameterList")
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            myChartId: UUID,
            partnerChartId: UUID,
            relationshipType: CompatibilityRelationshipType,
            score: Int,
            headline: String,
            subheadline: String,
            summary: String,
            totalAnalysis: String,
            analysisBasis: String,
            ohaengs: List<CompatibilityOhaeng>,
        ): SajuCompatibility =
            SajuCompatibility(
                id = id,
                memberId = memberId,
                myChartId = myChartId,
                partnerChartId = partnerChartId,
                relationshipType = relationshipType,
                score = score,
                headline = headline,
                subheadline = subheadline,
                summary = summary,
                totalAnalysis = totalAnalysis,
                analysisBasis = analysisBasis,
                ohaengs = ohaengs,
            )

        private fun validateScore(score: Int) {
            if (score !in MIN_SCORE..MAX_SCORE) {
                throw CompatibilityScoreOutOfRangeException()
            }
        }

        private fun validateOhaengs(ohaengs: List<CompatibilityOhaeng>) {
            // 서로 다른 오행이 5개인지뿐 아니라 총 개수도 5개인지 확인한다(중복 포함 6개 이상이 통과하는 것을 막는다).
            if (ohaengs.size != OHAENG_ELEMENT_COUNT || ohaengs.map { it.element }.distinct().size != OHAENG_ELEMENT_COUNT) {
                throw CompatibilityOhaengElementMismatchException()
            }
        }
    }
}
