package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.yearfortune.fixture.YearSelectionFortuneFixture
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
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
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

// 재조회 결과와 저장 후보를 구분해, 구현이 재조회 결과 대신 입력값을 반환해도 테스트가 통과하지 않도록 한다.
private val EXISTING_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000a1")
private val CANDIDATE_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000a2")

@ExperimentalUuidApi
class YearSelectionFortuneTransactionalStoreTest :
    DescribeSpec({
        val yearSelectionFortuneRepository = mockk<YearSelectionFortuneRepository>()
        val store = YearSelectionFortuneTransactionalStore(yearSelectionFortuneRepository)

        val memberId = Uuid.generateV7().toJavaUuid()
        val year = 2026

        afterTest { clearMocks(yearSelectionFortuneRepository) }

        beforeTest { every { yearSelectionFortuneRepository.lock(memberId, year) } just Runs }

        describe("findExistingWithLock") {
            it("(memberId, year) 락을 선점한 뒤 조회한다") {
                val existing = YearSelectionFortuneFixture.create(id = EXISTING_ID, memberId = memberId, year = year)
                every { yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) } returns existing

                val result = store.findExistingWithLock(memberId, year)

                result shouldBe existing
                verifyOrder {
                    yearSelectionFortuneRepository.lock(memberId, year)
                    yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year)
                }
            }
        }

        describe("saveIfAbsent") {
            context("락 재획득 후 재조회에서 이미 생성된 결과가 있으면") {
                it("저장하지 않고 재조회된 기존 결과를 반환한다(멱등)") {
                    val candidate = YearSelectionFortuneFixture.create(id = CANDIDATE_ID, memberId = memberId, year = year)
                    val existing = YearSelectionFortuneFixture.create(id = EXISTING_ID, memberId = memberId, year = year)
                    every { yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) } returns existing

                    val result = store.saveIfAbsent(candidate)

                    result.id shouldBe EXISTING_ID
                    verify(exactly = 0) { yearSelectionFortuneRepository.save(any()) }
                    verifyOrder {
                        yearSelectionFortuneRepository.lock(memberId, year)
                        yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year)
                    }
                }
            }

            context("재조회에서 결과가 없으면") {
                it("락 → 재조회 → 저장 순서로 저장한다") {
                    val toSave = YearSelectionFortuneFixture.create(id = CANDIDATE_ID, memberId = memberId, year = year)
                    every { yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) } returns null
                    every { yearSelectionFortuneRepository.save(toSave) } returns toSave

                    val result = store.saveIfAbsent(toSave)

                    result shouldBe toSave
                    verify(exactly = 1) { yearSelectionFortuneRepository.save(toSave) }
                    verifyOrder {
                        yearSelectionFortuneRepository.lock(memberId, year)
                        yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year)
                        yearSelectionFortuneRepository.save(toSave)
                    }
                }
            }
        }
    })
