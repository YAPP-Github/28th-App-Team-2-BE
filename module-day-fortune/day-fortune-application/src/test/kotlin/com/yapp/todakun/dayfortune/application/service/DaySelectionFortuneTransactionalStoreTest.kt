package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.fixture.DaySelectionFortuneFixture
import com.yapp.todakun.dayfortune.repository.DaySelectionFortuneRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

// 재조회 결과와 저장 후보를 구분해, 구현이 재조회 결과 대신 입력값을 반환해도 테스트가 통과하지 않도록 한다.
private val EXISTING_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000b1")
private val CANDIDATE_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000b2")

@ExperimentalUuidApi
class DaySelectionFortuneTransactionalStoreTest :
    DescribeSpec({
        val daySelectionFortuneRepository = mockk<DaySelectionFortuneRepository>()
        val store = DaySelectionFortuneTransactionalStore(daySelectionFortuneRepository)

        val memberId = Uuid.generateV7().toJavaUuid()
        val purpose = DaySelectionPurpose.TRAVEL
        val targetDate = LocalDate.now().plusDays(30)

        afterTest { clearMocks(daySelectionFortuneRepository) }

        beforeTest { every { daySelectionFortuneRepository.lock(memberId, purpose, targetDate) } just Runs }

        describe("findExistingWithLock") {
            it("(memberId, purpose, targetDate) 락을 선점한 뒤 조회한다") {
                val existing =
                    DaySelectionFortuneFixture.create(id = EXISTING_ID, memberId = memberId, purpose = purpose, targetDate = targetDate)
                every {
                    daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)
                } returns existing

                val result = store.findExistingWithLock(memberId, purpose, targetDate)

                result shouldBe existing
                verifyOrder {
                    daySelectionFortuneRepository.lock(memberId, purpose, targetDate)
                    daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)
                }
            }
        }

        describe("saveIfAbsent") {
            context("락 재획득 후 재조회에서 이미 생성된 결과가 있으면") {
                it("저장하지 않고 재조회된 기존 결과를 반환한다(멱등)") {
                    val candidate =
                        DaySelectionFortuneFixture.create(
                            id = CANDIDATE_ID,
                            memberId = memberId,
                            purpose = purpose,
                            targetDate = targetDate,
                        )
                    val existing =
                        DaySelectionFortuneFixture.create(
                            id = EXISTING_ID,
                            memberId = memberId,
                            purpose = purpose,
                            targetDate = targetDate,
                        )
                    every {
                        daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)
                    } returns existing

                    val result = store.saveIfAbsent(candidate)

                    result.id shouldBe EXISTING_ID
                    verify(exactly = 0) { daySelectionFortuneRepository.save(any()) }
                    verifyOrder {
                        daySelectionFortuneRepository.lock(memberId, purpose, targetDate)
                        daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)
                    }
                }
            }

            context("재조회에서 결과가 없으면") {
                it("락 → 재조회 → 저장 순서로 저장한다") {
                    val toSave =
                        DaySelectionFortuneFixture.create(
                            id = CANDIDATE_ID,
                            memberId = memberId,
                            purpose = purpose,
                            targetDate = targetDate,
                        )
                    every {
                        daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)
                    } returns null
                    every { daySelectionFortuneRepository.save(toSave) } returns toSave

                    val result = store.saveIfAbsent(toSave)

                    result shouldBe toSave
                    verify(exactly = 1) { daySelectionFortuneRepository.save(toSave) }
                    verifyOrder {
                        daySelectionFortuneRepository.lock(memberId, purpose, targetDate)
                        daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)
                        daySelectionFortuneRepository.save(toSave)
                    }
                }
            }
        }
    })
