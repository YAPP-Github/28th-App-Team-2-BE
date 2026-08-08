package com.yapp.todakun.compatibility.application

import com.yapp.todakun.compatibility.CompatibilityElement
import com.yapp.todakun.compatibility.CompatibilityOhaeng
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.SajuCompatibility
import com.yapp.todakun.compatibility.port.outbound.SajuCompatibilityRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

// 재조회 결과와 저장 후보를 구분해, 구현이 재조회 결과 대신 입력값을 반환해도 테스트가 통과하지 않도록 한다.
private val EXISTING_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000e1")
private val CANDIDATE_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000e2")

@ExperimentalUuidApi
class SajuCompatibilityTransactionalStoreTest :
    DescribeSpec({
        val sajuCompatibilityRepository = mockk<SajuCompatibilityRepository>()
        val store = SajuCompatibilityTransactionalStore(sajuCompatibilityRepository)

        val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001")
        val myChartId = UUID.fromString("018f0000-0000-7000-8000-0000000000c1")
        val partnerChartId = UUID.fromString("018f0000-0000-7000-8000-0000000000c2")

        afterTest { clearMocks(sajuCompatibilityRepository) }

        beforeTest { every { sajuCompatibilityRepository.lock(myChartId, partnerChartId) } just Runs }

        describe("findExistingWithLock") {
            it("(myChartId, partnerChartId) 락을 선점한 뒤 조회한다") {
                val existing = sajuCompatibility(EXISTING_ID, memberId, myChartId, partnerChartId)
                every { sajuCompatibilityRepository.findByMemberIdAndCharts(memberId, myChartId, partnerChartId) } returns existing

                val result = store.findExistingWithLock(memberId, myChartId, partnerChartId)

                result shouldBe existing
                verifyOrder {
                    sajuCompatibilityRepository.lock(myChartId, partnerChartId)
                    sajuCompatibilityRepository.findByMemberIdAndCharts(memberId, myChartId, partnerChartId)
                }
            }
        }

        describe("saveIfAbsent") {
            context("락 재획득 후 재조회에서 이미 생성된 결과가 있으면") {
                it("저장하지 않고 재조회된 기존 결과를 반환한다(멱등)") {
                    val candidate = sajuCompatibility(CANDIDATE_ID, memberId, myChartId, partnerChartId)
                    val existing = sajuCompatibility(EXISTING_ID, memberId, myChartId, partnerChartId)
                    every {
                        sajuCompatibilityRepository.findByMemberIdAndCharts(memberId, myChartId, partnerChartId)
                    } returns existing

                    val result = store.saveIfAbsent(candidate)

                    result.id shouldBe EXISTING_ID
                    verify(exactly = 0) { sajuCompatibilityRepository.save(any()) }
                    verifyOrder {
                        sajuCompatibilityRepository.lock(myChartId, partnerChartId)
                        sajuCompatibilityRepository.findByMemberIdAndCharts(memberId, myChartId, partnerChartId)
                    }
                }
            }

            context("재조회에서 결과가 없으면") {
                it("락 → 재조회 → 저장 순서로 저장한다") {
                    val toSave = sajuCompatibility(CANDIDATE_ID, memberId, myChartId, partnerChartId)
                    every { sajuCompatibilityRepository.findByMemberIdAndCharts(memberId, myChartId, partnerChartId) } returns null
                    every { sajuCompatibilityRepository.save(toSave) } returns toSave

                    val result = store.saveIfAbsent(toSave)

                    result shouldBe toSave
                    verify(exactly = 1) { sajuCompatibilityRepository.save(toSave) }
                    verifyOrder {
                        sajuCompatibilityRepository.lock(myChartId, partnerChartId)
                        sajuCompatibilityRepository.findByMemberIdAndCharts(memberId, myChartId, partnerChartId)
                        sajuCompatibilityRepository.save(toSave)
                    }
                }
            }
        }
    })

private fun sajuCompatibility(
    id: UUID,
    memberId: UUID,
    myChartId: UUID,
    partnerChartId: UUID,
): SajuCompatibility =
    SajuCompatibility.reconstitute(
        id = id,
        memberId = memberId,
        myChartId = myChartId,
        partnerChartId = partnerChartId,
        relationshipType = CompatibilityRelationshipType.LOVER,
        score = 85,
        headline = "함께할수록 빛나는 궁합",
        subheadline = "함께 있을 때, 편안함이 커지는 사이예요.",
        summary = "두 분은 서로의 부족한 기운을 보완합니다.",
        totalAnalysis = "총운 분석 내용",
        analysisBasis = "사주 팔자 기반",
        ohaengs = CompatibilityElement.entries.map { CompatibilityOhaeng(it, 20) },
    )
